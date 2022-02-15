package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.pattern.StatusReply
import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.pdnd.interop.commons.files.service.FileManager
import it.pagopa.pdnd.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.pdnd.interop.commons.utils.AkkaUtils
import it.pagopa.pdnd.interop.commons.utils.TypeConversions._
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PublicApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system._
import it.pagopa.pdnd.interop.uservice.partymanagement.error.PartyManagementErrors._
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence._
import org.slf4j.LoggerFactory

import java.io.File
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class PublicApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]],
  fileManager: FileManager
)(implicit ec: ExecutionContext)
    extends PublicApiService {

  val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](LoggerFactory.getLogger(this.getClass))

  private val settings: ClusterShardingSettings = entity.settings match {
    case None    => ClusterShardingSettings(system)
    case Some(s) => s
  }

  def getCommander(entityId: String): EntityRef[Command] =
    sharding.entityRefFor(PartyPersistentBehavior.TypeKey, AkkaUtils.getShard(entityId, settings.numberOfShards))

  /** Code: 200, Message: successful operation, DataType: TokenInfo
    * Code: 404, Message: Token not found, DataType: Problem
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def getToken(tokenId: String)(implicit
    toEntityMarshallerTokenInfo: ToEntityMarshaller[TokenInfo],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info("Getting token {}", tokenId)
    val result: Future[TokenInfo] = for {
      tokenIdUUID <- tokenId.toFutureUUID
      result      <- getCommander(tokenId).ask(ref => GetToken(tokenIdUUID, ref))
      token       <- result.toFuture(TokenNotFound(tokenId))
    } yield TokenInfo(id = token.id, checksum = token.checksum, legals = token.legals.map(_.toApi))

    onComplete(result) {
      case Success(token) =>
        getToken200(token)
      case Failure(ex: TokenNotFound) =>
        logger.error("Getting token failed", ex)
        getToken404(problemOf(StatusCodes.NotFound, ex))
      case Failure(ex) =>
        logger.error("Getting token failed", ex)
        complete(problemOf(StatusCodes.InternalServerError, GetTokenFatalError(tokenId, ex.getMessage)))
    }
  }

  /** Code: 201, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def consumeToken(tokenId: String, doc: (FileInfo, File))(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info("Consuming token {}", tokenId)
    val results: Future[Seq[StatusReply[Unit]]] = for {
      tokenIdUUID <- tokenId.toFutureUUID
      found       <- getCommander(tokenId).ask(ref => GetToken(tokenIdUUID, ref))
      token       <- found.toFuture(TokenNotFound(tokenId))
      results <-
        if (token.isValid) confirmRelationships(token, doc)
        else
          processRelationships(token, RejectPartyRelationship).flatMap(_ => Future.failed(TokenExpired(tokenId)))
    } yield results

    onComplete(results) {
      case Success(statusReplies) if statusReplies.exists(_.isError) =>
        val errors: String =
          statusReplies.filter(_.isError).flatMap(sr => Option(sr.getError.getMessage)).mkString("\n")
        logger.error("Consuming token failed: {}", errors)
        consumeToken400(problemOf(StatusCodes.BadRequest, ConsumeTokenBadRequest(errors)))
      case Success(_) => consumeToken201
      case Failure(ex: TokenNotFound) =>
        logger.error("Token not found", ex)
        consumeToken404(problemOf(StatusCodes.NotFound, ConsumeTokenError(ex.getMessage)))
      case Failure(ex) =>
        logger.error("Consuming token failed", ex)
        consumeToken400(problemOf(StatusCodes.BadRequest, ConsumeTokenError(ex.getMessage)))
    }

  }

  /** Code: 201, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def invalidateToken(
    tokenId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {
    logger.info("Invalidating token {}", tokenId)
    val results: Future[Seq[StatusReply[Unit]]] = for {
      tokenIdUUID <- tokenId.toFutureUUID
      found       <- getCommander(tokenId).ask(ref => GetToken(tokenIdUUID, ref))
      token       <- found.toFuture(TokenNotFound(tokenId))
      results     <- processRelationships(token, RejectPartyRelationship)
    } yield results

    onComplete(results) {
      case Success(statusReplies) if statusReplies.exists(_.isError) =>
        val errors: String =
          statusReplies.filter(_.isError).flatMap(sr => Option(sr.getError.getMessage)).mkString("\n")
        logger.error("Invalidating token failed: {}", errors)
        invalidateToken400(problemOf(StatusCodes.BadRequest, InvalidateTokenBadRequest(errors)))
      case Success(_) => invalidateToken200
      case Failure(ex: TokenNotFound) =>
        logger.error("Token not found", ex)
        invalidateToken404(problemOf(StatusCodes.NotFound, ConsumeTokenError(ex.getMessage)))
      case Failure(ex) =>
        logger.error("Invalidating token failed", ex)
        invalidateToken400(problemOf(StatusCodes.BadRequest, InvalidateTokenError(ex.getMessage)))
    }

  }

  private def processRelationships(
    token: Token,
    commandFunc: (UUID, ActorRef[StatusReply[Unit]]) => Command
  ): Future[Seq[StatusReply[Unit]]] = {
    for {
      results <- Future.traverse(token.legals) { partyRelationshipBinding =>
        getCommander(partyRelationshipBinding.partyId.toString).ask((ref: ActorRef[StatusReply[Unit]]) =>
          commandFunc(partyRelationshipBinding.relationshipId, ref)
        )
      } //TODO atomic?
    } yield results
  }

  private def confirmRelationships(token: Token, fileParts: (FileInfo, File)): Future[Seq[StatusReply[Unit]]] = {
    for {
      filePath <- fileManager.store(ApplicationConfiguration.storageContainer, ApplicationConfiguration.contractPath)(
        token.id,
        fileParts
      )
      results <- Future.traverse(token.legals) { partyRelationshipBinding =>
        getCommander(partyRelationshipBinding.partyId.toString).ask((ref: ActorRef[StatusReply[Unit]]) =>
          ConfirmPartyRelationship(partyRelationshipBinding.relationshipId, filePath, fileParts._1, token.id, ref)
        )
      } //TODO atomic?
    } yield results
  }

  /** Code: 200, Message: successful operation, DataType: TokenInfo
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    * Code: 404, Message: Token not found, DataType: Problem
    */
  override def verifyToken(tokenId: String)(implicit
    toEntityMarshallerTokenInfo: ToEntityMarshaller[TokenInfo],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val commanders = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    def getRelationship(relationshipId: UUID): Future[PersistedPartyRelationship] = {
      for {
        results      <- commanders.traverse(_.ask(ref => GetPartyRelationshipById(relationshipId, ref)))
        relationship <- results.find(_.isDefined).flatten.toFuture(GetRelationshipNotFound(relationshipId.toString))
      } yield relationship
    }

    val result: Future[TokenInfo] =
      for {
        uuid          <- tokenId.toFutureUUID
        found         <- getCommander(tokenId).ask(ref => GetToken(uuid, ref))
        token         <- found.toFuture(TokenNotFound(tokenId))
        relationships <- token.legals.traverse(r => getRelationship(r.relationshipId))
        _             <- isTokenNotConsumed(tokenId, relationships)
      } yield TokenInfo(id = token.id, checksum = token.checksum, legals = token.legals.map(_.toApi))

    onComplete(result) {
      case Success(tokenInfo) => verifyToken200(tokenInfo)
      case Failure(ex: TokenNotFound) =>
        logger.error("Token not found", ex)
        verifyToken404(problemOf(StatusCodes.NotFound, ex))
      case Failure(ex: TokenAlreadyConsumed) =>
        logger.error("Token already consumed", ex)
        verifyToken409(problemOf(StatusCodes.Conflict, ex))
      case Failure(ex: GetRelationshipNotFound) =>
        logger.error("Missing token relationships", ex)
        verifyToken404(problemOf(StatusCodes.BadRequest, ex))
      case Failure(ex) =>
        logger.error("Verifying token failed", ex)
        complete(problemOf(StatusCodes.InternalServerError, TokenVerificationFatalError(tokenId, ex.getMessage)))
    }
  }

  private def isTokenNotConsumed(tokenId: String, relationships: Seq[PersistedPartyRelationship]): Future[Unit] = {
    val error: Either[Throwable, Unit] = Left(TokenAlreadyConsumed(tokenId))
    error
      .unlessA(relationships.nonEmpty && relationships.forall(_.state == PersistedPartyRelationshipState.Pending))
      .toFuture

  }

}
