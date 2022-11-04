package it.pagopa.interop.partymanagement.api.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.partymanagement.api.ExternalApiService
import it.pagopa.interop.partymanagement.common.system._
import it.pagopa.interop.partymanagement.error.PartyManagementErrors._
import it.pagopa.interop.partymanagement.model._
import it.pagopa.interop.partymanagement.model.party._
import it.pagopa.interop.partymanagement.model.persistence._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ExternalApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]]
)(implicit ec: ExecutionContext)
    extends ExternalApiService {

  val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](LoggerFactory.getLogger(this.getClass))

  private val settings: ClusterShardingSettings = entity.settings match {
    case None    => ClusterShardingSettings(system)
    case Some(s) => s
  }

  def getCommander(entityId: String): EntityRef[Command] =
    sharding.entityRefFor(PartyPersistentBehavior.TypeKey, AkkaUtils.getShard(entityId, settings.numberOfShards))

  /**
    * Code: 200, Message: Institution, DataType: Institution
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Institution not found, DataType: Problem
    */
  override def getInstitutionByExternalId(externalId: String)(implicit
    toEntityMarshallerInstitution: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: scala.Seq[(String, String)]
  ): Route = {
    logger.info("Getting institution with external id {}", externalId)

    val commanders = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val institution: Future[InstitutionParty] = for {
      shardOrgs <- Future.traverse(commanders)(_.ask(ref => GetInstitutionByExternalId(externalId, ref)))
      result    <- shardOrgs.flatten.headOption.toFuture(InstitutionNotFound(externalId))
    } yield result

    onComplete(institution) {
      case Success(statusReply)             =>
        Party
          .convertToApi(statusReply)
          .swap
          .fold(
            _ => {
              logger.error(s"Error while getting institution with external id $externalId - Not found")
              getInstitutionByExternalId400(problemOf(StatusCodes.BadRequest, InstitutionNotFound(externalId)))
            },
            institution => getInstitutionByExternalId200(institution)
          )
      case Failure(ex: InstitutionNotFound) =>
        logger.info(s"Institution with external id $externalId not found", ex)
        getInstitutionByExternalId404(problemOf(StatusCodes.NotFound, ex))
      case Failure(ex)                      =>
        logger.error(s"Error while getting institution with external id $externalId", ex)
        getInstitutionByExternalId400(problemOf(StatusCodes.BadRequest, InstitutionBadRequest))
    }
  }

  /**
    * Code: 200, Message: Institutions, DataType: Institutions
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Relationships not found, DataType: Problem
    */
  override def getInstitutionsByProductId(productId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions],
    contexts: scala.Seq[(String, String)]
  ): Route = {
    logger.info(s"Getting institution with product id $productId")

    val commanders = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val institutions: Future[List[Institution]] = for {
      shardOrgs <- Future.traverse(commanders)(_.ask(ref => GetInstitutionsByProductId(productId, ref)))
    } yield shardOrgs.flatMap(_.toList)

    onComplete(institutions) {
      case Success(institutions)            =>
        logger.info(s"Institution with product id $productId not found")
        getInstitutionsByProductId200(Institutions(institutions))
      case Failure(ex: InstitutionNotFound) =>
        logger.info(s"Institution with product id $productId not found", ex)
        getInstitutionsByProductId404(problemOf(StatusCodes.NotFound, ex))
      case Failure(ex)                      =>
        logger.error(s"Error while getting institution with product id $productId", ex)
        getInstitutionsByProductId400(problemOf(StatusCodes.BadRequest, InstitutionBadRequest))
    }
  }
}
