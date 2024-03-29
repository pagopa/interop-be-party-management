package it.pagopa.interop.partymanagement.api.impl

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.{ContentType, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.pattern.StatusReply
import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.partymanagement.api.PublicApiService
import it.pagopa.interop.partymanagement.common.system.{ApplicationConfiguration, _}
import it.pagopa.interop.partymanagement.error.PartyManagementErrors._
import it.pagopa.interop.partymanagement.model._
import it.pagopa.interop.partymanagement.model.party.PersistedPartyRelationshipState.{Pending, ToBeValidated}
import it.pagopa.interop.partymanagement.model.party._
import it.pagopa.interop.partymanagement.model.persistence._
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

    val commanders: List[EntityRef[Command]] = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val getRelationship: UUID => Future[PersistedPartyRelationship] = getRelationshipsFunc(commanders)

    val result: Future[TokenInfo] = for {
      tokenIdUUID   <- tokenId.toFutureUUID
      result        <- getCommander(tokenId).ask(ref => GetToken(tokenIdUUID, ref))
      token         <- result.toFuture(TokenNotFound(tokenId))
      relationships <- Future.traverse(token.legals)(r => getRelationship(r.relationshipId))
    } yield TokenInfo(
      id = token.id,
      checksum = token.checksum,
      legals =
        relationships.map(rl => RelationshipBinding(partyId = rl.from, relationshipId = rl.id, role = rl.role.toApi))
    )

    onComplete(result) {
      case Success(token)             =>
        getToken200(token)
      case Failure(ex: TokenNotFound) =>
        logger.error(s"Getting token failed", ex)
        getToken404(problemOf(StatusCodes.NotFound, ex))
      case Failure(ex)                =>
        logger.error(s"Getting token failed", ex)
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
      results     <-
        if (token.isValid) confirmRelationships(token, doc)
        else
          processRelationships(token, RejectPartyRelationship).flatMap(_ => Future.failed(TokenExpired(tokenId)))
    } yield results

    onComplete(results) {
      case Success(statusReplies) if statusReplies.exists(_.isError) =>
        val errors: String =
          statusReplies.filter(_.isError).flatMap(sr => Option(sr.getError.getMessage)).mkString("\n")
        logger.error(s"Consuming token failed: $errors")
        consumeToken400(problemOf(StatusCodes.BadRequest, ConsumeTokenBadRequest(errors)))
      case Success(_)                                                => consumeToken201
      case Failure(ex: TokenNotFound)                                =>
        logger.error(s"Token not found", ex)
        consumeToken404(problemOf(StatusCodes.NotFound, ConsumeTokenError(ex.getMessage)))
      case Failure(ex)                                               =>
        logger.error(s"Consuming token failed", ex)
        consumeToken400(problemOf(StatusCodes.BadRequest, ConsumeTokenError(ex.getMessage)))
    }

  }

  /**
    * Code: 201, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    * Code: 404, Message: Token not found, DataType: Problem
    */
  override def consumeTokenWithoutContract(tokenId: String, onboardingContract: OnboardingContract)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info("Consuming token {}", tokenId)
    val results: Future[Seq[StatusReply[Unit]]] = for {
      tokenIdUUID <- tokenId.toFutureUUID
      found       <- getCommander(tokenId).ask(ref => GetToken(tokenIdUUID, ref))
      token       <- found.toFuture(TokenNotFound(tokenId))
      results     <-
        if (token.isValid) confirmRelationshipsWithoutContract(token, onboardingContract)
        else
          processRelationships(token, RejectPartyRelationship).flatMap(_ => Future.failed(TokenExpired(tokenId)))
    } yield results

    onComplete(results) {
      case Success(statusReplies) if statusReplies.exists(_.isError) =>
        val errors: String =
          statusReplies.filter(_.isError).flatMap(sr => Option(sr.getError.getMessage)).mkString("\n")
        logger.error(s"Consuming token failed: $errors")
        consumeTokenWithoutContract400(problemOf(StatusCodes.BadRequest, ConsumeTokenBadRequest(errors)))
      case Success(_)                                                => consumeToken201
      case Failure(ex: TokenNotFound)                                =>
        logger.error(s"Token not found", ex)
        consumeTokenWithoutContract404(problemOf(StatusCodes.NotFound, ConsumeTokenError(ex.getMessage)))
      case Failure(ex)                                               =>
        logger.error(s"Consuming token failed", ex)
        consumeTokenWithoutContract400(problemOf(StatusCodes.BadRequest, ConsumeTokenError(ex.getMessage)))
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
        logger.error(s"Invalidating token failed: $errors")
        invalidateToken400(problemOf(StatusCodes.BadRequest, InvalidateTokenBadRequest(errors)))
      case Success(_)                                                => invalidateToken200
      case Failure(ex: TokenNotFound)                                =>
        logger.error(s"Token not found", ex)
        invalidateToken404(problemOf(StatusCodes.NotFound, ConsumeTokenError(ex.getMessage)))
      case Failure(ex)                                               =>
        logger.error(s"Invalidating token failed", ex)
        invalidateToken400(problemOf(StatusCodes.BadRequest, InvalidateTokenError(ex.getMessage)))
    }

  }

  /**
    * Code: 201, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    * Code: 404, Message: Token not found, DataType: Problem
    */
  override def deleteToken(
    tokenId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {
    logger.info("Deleting token {}", tokenId)
    val results: Future[Seq[StatusReply[Unit]]] = for {
      tokenIdUUID <- tokenId.toFutureUUID
      found       <- getCommander(tokenId).ask(ref => GetToken(tokenIdUUID, ref))
      token       <- found.toFuture(TokenNotFound(tokenId))
      results     <- processRelationships(token, DeletePartyRelationship)
    } yield results

    onComplete(results) {
      case Success(statusReplies) if statusReplies.exists(_.isError) =>
        val errors: String =
          statusReplies.filter(_.isError).flatMap(sr => Option(sr.getError.getMessage)).mkString("\n")
        logger.error(s"Deleting token failed: $errors")
        deleteToken400(problemOf(StatusCodes.BadRequest, DeleteTokenBadRequest(errors)))
      case Success(_)                                                => deleteToken201
      case Failure(ex: TokenNotFound)                                =>
        logger.error(s"Token not found", ex)
        deleteToken404(problemOf(StatusCodes.NotFound, ConsumeTokenError(ex.getMessage)))
      case Failure(ex)                                               =>
        logger.error(s"Deleting token failed", ex)
        deleteToken400(problemOf(StatusCodes.BadRequest, DeleteTokenError(ex.getMessage)))
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
      } // TODO atomic?
    } yield results
  }

  private def confirmRelationships(token: Token, fileParts: (FileInfo, File)): Future[Seq[StatusReply[Unit]]] = {
    for {
      filePath <- fileManager.store(ApplicationConfiguration.storageContainer, ApplicationConfiguration.contractPath)(
        token.id,
        fileParts
      )
      results  <- Future.traverse(token.legals) { partyRelationshipBinding =>
        getCommander(partyRelationshipBinding.partyId.toString).ask((ref: ActorRef[StatusReply[Unit]]) =>
          ConfirmPartyRelationship(partyRelationshipBinding.relationshipId, filePath, fileParts._1, token.id, ref)
        )
      } // TODO atomic?
      _        <- updateInstitutionOnConfirmation(token)
    } yield results
  }

  private def confirmRelationshipsWithoutContract(
    token: Token,
    onboardingContract: OnboardingContract
  ): Future[Seq[StatusReply[Unit]]] = {
    for {
      results <- Future.traverse(token.legals) { partyRelationshipBinding =>
        getCommander(partyRelationshipBinding.partyId.toString).ask((ref: ActorRef[StatusReply[Unit]]) =>
          ConfirmPartyRelationship(
            partyRelationshipBinding.relationshipId,
            "",
            FileInfo(
              onboardingContract.filePath,
              onboardingContract.fileName,
              ContentType.WithFixedCharset(MediaTypes.`application/json`)
            ),
            token.id,
            ref
          )
        )
      } // TODO atomic?
      _       <- updateInstitutionOnConfirmation(token)
    } yield results
  }

  private def updateInstitutionOnConfirmation(token: Token) = {
    for {
      relationships    <- Future.traverse(token.legals)(legal =>
        getCommander(legal.partyId.toString).ask(ref => GetPartyRelationshipById(legal.relationshipId, ref))
      )
      manager          <- relationships
        .find(_.exists(_.role == PersistedPartyRole.Manager))
        .flatten
        .toFuture(ManagerNotSupplied(token.id.toString))
      party            <- getCommander(manager.to.toString).ask(ref => GetParty(manager.to, ref))
      institutionParty <- Party.extractInstitutionParty(partyId = manager.to.toString, party = party)
      _                <- updateInstitutionWithRelationship(institutionParty, manager)
    } yield ()
  }

  private def updateInstitutionWithRelationship(
    institutionParty: InstitutionParty,
    relationship: PersistedPartyRelationship
  ): Future[StatusReply[Party]] = {
    val institutionPartyUpdate  = updateWithInstitutionUpdate(institutionParty, relationship)
    val institutionPartyProduct = updateWithInstitutionProductInfo(institutionPartyUpdate, relationship)
    Option
      .when(relationship.institutionUpdate.isDefined || relationship.billing.isDefined) {
        getCommander(institutionPartyProduct.id.toString).ask(ref => UpdateParty(institutionPartyProduct, ref))
      }
      .getOrElse(Future.successful(StatusReply.Success[Party](institutionParty)))
  }

  private def updateWithInstitutionUpdate(
    institutionParty: InstitutionParty,
    relationship: PersistedPartyRelationship
  ): InstitutionParty = {
    relationship.institutionUpdate.fold(institutionParty) { institutionUpdate =>
      if (institutionParty.origin == ipaOrigin)
        institutionParty
          .copy(
            institutionType = institutionUpdate.institutionType.orElse(institutionParty.institutionType),
            geographicTaxonomies = institutionUpdate.geographicTaxonomies,
            supportEmail = institutionUpdate.supportEmail
          )
      else
        institutionParty.copy(
          institutionType = institutionUpdate.institutionType.orElse(institutionParty.institutionType),
          address = institutionUpdate.address.getOrElse(institutionParty.address),
          taxCode = institutionUpdate.taxCode.getOrElse(institutionParty.taxCode),
          description = institutionUpdate.description.getOrElse(institutionParty.description),
          digitalAddress = institutionUpdate.digitalAddress.getOrElse(institutionParty.digitalAddress),
          zipCode = institutionUpdate.zipCode.getOrElse(institutionParty.zipCode),
          geographicTaxonomies = institutionUpdate.geographicTaxonomies,
          paymentServiceProvider = institutionUpdate.paymentServiceProvider,
          dataProtectionOfficer = institutionUpdate.dataProtectionOfficer,
          rea = institutionUpdate.rea,
          shareCapital = institutionUpdate.shareCapital,
          businessRegisterPlace = institutionUpdate.businessRegisterPlace,
          supportEmail = institutionUpdate.supportEmail
        )
    }
  }

  private def updateWithInstitutionProductInfo(
    institutionParty: InstitutionParty,
    relationship: PersistedPartyRelationship
  ): InstitutionParty = {
    relationship.billing.fold(institutionParty) { billing =>
      val productId          = relationship.product.id
      val institutionProduct = institutionParty.products
        .find(_.product == productId)
        .map(_.copy(pricingPlan = relationship.pricingPlan, billing = billing))
        .getOrElse(
          PersistedInstitutionProduct(product = productId, pricingPlan = relationship.pricingPlan, billing = billing)
        )
      institutionParty.copy(products = institutionParty.products.filter(_.product != productId) + institutionProduct)
    }
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

    val commanders: List[EntityRef[Command]] = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val getRelationships: UUID => Future[PersistedPartyRelationship] = getRelationshipsFunc(commanders)

    val result: Future[TokenInfo] =
      for {
        uuid          <- tokenId.toFutureUUID
        found         <- getCommander(tokenId).ask(ref => GetToken(uuid, ref))
        token         <- found.toFuture(TokenNotFound(tokenId))
        relationships <- Future.traverse(token.legals)(r => getRelationships(r.relationshipId))
        _             <- isTokenNotConsumed(tokenId, relationships)
      } yield TokenInfo(
        id = token.id,
        checksum = token.checksum,
        legals =
          relationships.map(rl => RelationshipBinding(partyId = rl.from, relationshipId = rl.id, role = rl.role.toApi))
      )

    onComplete(result) {
      case Success(tokenInfo)                   => verifyToken200(tokenInfo)
      case Failure(ex: TokenNotFound)           =>
        logger.error(s"Token not found", ex)
        verifyToken404(problemOf(StatusCodes.NotFound, ex))
      case Failure(ex: TokenAlreadyConsumed)    =>
        logger.error(s"Token already consumed", ex)
        verifyToken409(problemOf(StatusCodes.Conflict, ex))
      case Failure(ex: GetRelationshipNotFound) =>
        logger.error(s"Missing token relationships", ex)
        verifyToken400(problemOf(StatusCodes.BadRequest, ex))
      case Failure(ex)                          =>
        logger.error(s"Verifying token failed", ex)
        complete(problemOf(StatusCodes.InternalServerError, TokenVerificationFatalError(tokenId, ex.getMessage)))
    }
  }

  private def getRelationshipsFunc(commanders: List[EntityRef[Command]]): UUID => Future[PersistedPartyRelationship] =
    relationshipId => {
      for {
        results      <- Future.traverse(commanders)(_.ask(ref => GetPartyRelationshipById(relationshipId, ref)))
        relationship <- results.find(_.isDefined).flatten.toFuture(GetRelationshipNotFound(relationshipId.toString))
      } yield relationship
    }

  private def isTokenNotConsumed(tokenId: String, relationships: Seq[PersistedPartyRelationship]): Future[Unit] = {
    val error: Either[Throwable, Unit] = Left(TokenAlreadyConsumed(tokenId))
    error
      .unlessA(
        relationships.nonEmpty &&
          (relationships.forall(s => s.state == Pending || s.state == ToBeValidated))
      )
      .toFuture
  }

  /**
    * Code: 200, Message: successful operation, DataType: InstitutionId
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Relationship not found, DataType: Problem
    */
  override def getInstitutionIdFromRelationshipId(relationshipId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerInstitutionId: ToEntityMarshaller[InstitutionId],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info("Getting institutionId with relation id {}", relationshipId)

    val commanders = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val result: Future[Option[InstitutionId]] =
      for {
        uuid    <- relationshipId.toFutureUUID
        results <- Future.traverse(commanders)(_.ask(ref => GetPartyRelationshipById(uuid, ref)))
        maybePartyRelationship = results.find(_.isDefined).flatten
        partyInstitutionId     = maybePartyRelationship.map(_.toInstitutionId)
      } yield partyInstitutionId

    onComplete(result) {
      case Success(Some(institutionId)) => getInstitutionIdFromRelationshipId200(institutionId)
      case Success(None)                =>
        logger.error(s"Error while getting relationship with id $relationshipId - Not found")
        getInstitutionIdFromRelationshipId404(problemOf(StatusCodes.NotFound, GetRelationshipNotFound(relationshipId)))
      case Failure(ex)                  =>
        logger.error(s"Error while getting relationship with id $relationshipId", ex)
        getInstitutionIdFromRelationshipId400(problemOf(StatusCodes.BadRequest, GetRelationshipError))
    }
  }

}
