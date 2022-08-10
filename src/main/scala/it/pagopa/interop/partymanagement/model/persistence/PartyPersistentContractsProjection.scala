package it.pagopa.interop.partymanagement.model.persistence

import akka.Done
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.ProjectionId
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.{ExactlyOnceProjection, SourceProvider}
import akka.projection.slick.{SlickHandler, SlickProjection}
import cats.syntax.all._
import it.pagopa.interop.commons.queue.kafka.KafkaPublisher
import it.pagopa.interop.commons.utils.AkkaUtils
import it.pagopa.interop.commons.utils.SprayCommonFormats.{offsetDateTimeFormat, uuidFormat}
import it.pagopa.interop.partymanagement.common.system._
import it.pagopa.interop.partymanagement.error.PartyManagementErrors.GetInstitutionError
import it.pagopa.interop.partymanagement.model.PartyRole
import it.pagopa.interop.partymanagement.model.party.{
  InstitutionOnboarded,
  InstitutionOnboardedBilling,
  InstitutionOnboardedNotification,
  InstitutionOnboardedNotificationObj,
  InstitutionParty,
  Party
}
import it.pagopa.interop.partymanagement.service.{InstitutionService, RelationshipService}
import org.slf4j.{Logger, LoggerFactory}
import slick.basic.DatabaseConfig
import slick.dbio._
import slick.jdbc.JdbcProfile
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class PartyPersistentContractsProjection(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]],
  relationshipService: RelationshipService,
  institutionService: InstitutionService,
  dbConfig: DatabaseConfig[JdbcProfile],
  datalakeContractsPublisher: KafkaPublisher
) {
  implicit val ec: ExecutionContext = system.executionContext

  def sourceProvider(tag: String): SourceProvider[Offset, EventEnvelope[Event]] =
    EventSourcedProvider
      .eventsByTag[Event](system, readJournalPluginId = JdbcReadJournal.Identifier, tag = tag)

  def projection(tag: String): ExactlyOnceProjection[Offset, EventEnvelope[Event]] = {
    implicit val as: ActorSystem[_] = system
    SlickProjection.exactlyOnce(
      projectionId = ProjectionId("party-contracts-projections", tag),
      sourceProvider = sourceProvider(tag),
      handler = () =>
        new ProjectionContractsHandler(system, sharding, entity, relationshipService, institutionService)(
          tag,
          datalakeContractsPublisher
        ),
      databaseConfig = dbConfig
    )
  }
}

class ProjectionContractsHandler(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]],
  relationshipService: RelationshipService,
  institutionService: InstitutionService
)(tag: String, datalakeContractsPublisher: KafkaPublisher)(implicit ec: ExecutionContext)
    extends SlickHandler[EventEnvelope[Event]] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val settings: ClusterShardingSettings = entity.settings match {
    case None    => ClusterShardingSettings(system)
    case Some(s) => s
  }

  def getCommander(entityId: String): EntityRef[Command] =
    sharding.entityRefFor(PartyPersistentBehavior.TypeKey, AkkaUtils.getShard(entityId, settings.numberOfShards))

  override def process(envelope: EventEnvelope[Event]): DBIO[Done] = {
    envelope.event match {
      case event: PartyRelationshipConfirmed =>
        checkRelationshipConfirmed(event)
      case _                                 =>
        logger.debug("This is the envelope event payload > {} On tagged projection > {}", envelope.event, tag)
        DBIOAction.successful(Done)
    }
  }

  private def checkRelationshipConfirmed(event: PartyRelationshipConfirmed): DBIO[Done] = {
    logger.debug(s"projecting confirmation of relationship having id ${event.partyRelationshipId}")
    DBIOAction.from {
      val future = relationshipService
        .getRelationshipById(event.partyRelationshipId)
        .map(optRel => {
          optRel
            .filter(r => PartyRole.MANAGER == r.role)
            .map(r => notifyInstitutionOnboarded(r.to, r.product.id, event))
            .getOrElse(DBIOAction.successful(Done))
        })
      future.onComplete {
        case Failure(e) =>
          logger.error(
            s"Error projecting confirmation of relationshing having id ${event.partyRelationshipId} on queue",
            e
          )
          DBIOAction.failed(e)
        case Success(_) =>
          DBIOAction.successful(Done)
      }
      future.as(Done)
    }
  }

  def notifyInstitutionOnboarded(
    institutionId: UUID,
    productId: String,
    managerRelationshipConfirm: PartyRelationshipConfirmed
  )(implicit ec: ExecutionContext): Future[Any] = {
    logger.debug(s"confirming institution having id $institutionId")
    for {
      institutionOpt <- institutionService.getInstitutionById(institutionId)
      institution    <- unpackInstitutionParty(institutionOpt)
      notification = InstitutionOnboardedNotificationObj.toNotification(
        institution,
        productId,
        managerRelationshipConfirm
      )
      sendResult <- datalakeContractsPublisher.send(notification)
    } yield sendResult
  }

  private def unpackInstitutionParty(institutionOpt: Option[Party]): Future[InstitutionParty] = {
    institutionOpt.orNull match {
      case institutionParty: InstitutionParty => Future.successful(institutionParty)
      case _                                  => Future.failed(GetInstitutionError)
    }
  }

  implicit val institutionOnboardedFormat: RootJsonFormat[InstitutionOnboarded] = jsonFormat7(InstitutionOnboarded)
  implicit val institutionOnboardedBillingFormat: RootJsonFormat[InstitutionOnboardedBilling]           = jsonFormat3(
    InstitutionOnboardedBilling
  )
  implicit val institutionOnboardedNotificationFormat: RootJsonFormat[InstitutionOnboardedNotification] = jsonFormat11(
    InstitutionOnboardedNotification
  )
}
