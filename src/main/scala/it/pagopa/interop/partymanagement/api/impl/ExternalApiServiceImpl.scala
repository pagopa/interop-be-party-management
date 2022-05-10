package it.pagopa.interop.partymanagement.api.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import cats.implicits.toTraverseOps
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

  override def getInstitutionByExternalId(externalId: String)(implicit
    toEntityMarshallerInstitution: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info("Getting institution with external id {}", externalId)

    val commanders = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val institution: Future[InstitutionParty] = for {
      shardOrgs <- commanders.traverse(_.ask(ref => GetInstitutionByExternalId(externalId, ref)))
      result    <- shardOrgs.flatten.headOption.toFuture(InstitutionNotFound(externalId))
    } yield result

    onComplete(institution) {
      case Success(statusReply) =>
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
      case Failure(ex)          =>
        logger.error(s"Error while getting institution with external id $externalId", ex)
        getInstitutionByExternalId400(problemOf(StatusCodes.BadRequest, InstitutionBadRequest))
    }

  }

}
