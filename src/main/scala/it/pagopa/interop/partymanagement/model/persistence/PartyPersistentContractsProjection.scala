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
import it.pagopa.interop.commons.queue.kafka.KafkaPublisher
import it.pagopa.interop.commons.utils.AkkaUtils
import it.pagopa.interop.commons.utils.TypeConversions.OptionOps
import it.pagopa.interop.commons.utils.SprayCommonFormats.{offsetDateTimeFormat, uuidFormat}
import it.pagopa.interop.partymanagement.common.system._
import it.pagopa.interop.partymanagement.error.PartyManagementErrors.{GetInstitutionError, GetRelationshipNotFound}
import it.pagopa.interop.partymanagement.model.{PartyRole, Relationship}
import it.pagopa.interop.partymanagement.model.party.{
  InstitutionOnboarded,
  InstitutionOnboardedBilling,
  InstitutionOnboardedNotification,
  InstitutionOnboardedNotificationObj,
  InstitutionParty,
  PSP,
  Party,
  QueueEvent
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
import akka.projection.HandlerRecoveryStrategy

import scala.concurrent.duration._

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
    SlickProjection
      .exactlyOnce(
        projectionId = ProjectionId("party-contracts-projections", tag),
        sourceProvider = sourceProvider(tag),
        handler = () =>
          new ProjectionContractsHandler(system, sharding, entity, relationshipService, institutionService)(
            tag,
            datalakeContractsPublisher
          ),
        databaseConfig = dbConfig
      )
      .withRecoveryStrategy(HandlerRecoveryStrategy.retryAndFail(6, 10.second))
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
      case event: PartyRelationshipConfirmed     =>
        checkRelationshipConfirmed(PartyRelationshipWithId(event.partyRelationshipId), QueueEvent.ADD)
      case event: PartyRelationshipUpdateBilling =>
        checkRelationshipConfirmed(PartyRelationshipWithId(event.partyRelationshipId), QueueEvent.UPDATE)
      case _                                     =>
        logger.debug("This is the envelope event payload > {} On tagged projection > {}", envelope.event, tag)
        DBIOAction.successful(Done)
    }
  }

  private def checkRelationshipConfirmed(event: PartyRelationshipWithId, queueEvent: QueueEvent): DBIO[Done] = {
    logger.info(s"projecting confirmation of relationship having id ${event.partyRelationshipId}") // apz debug
    val result = for {
      found             <- relationshipService.getRelationshipById(event.partyRelationshipId)
      partyRelationship <- found.toFuture(GetRelationshipNotFound(event.partyRelationshipId.toString))
      _                 <-
        if (partyRelationship.role == PartyRole.MANAGER)
          notifyInstitutionOnboarded(partyRelationship.to, partyRelationship.product.id, partyRelationship, queueEvent)
        else Future.unit
    } yield Done

    result.onComplete {
      case Failure(e) =>
        logger.info( // apz debug
          s"Error projecting confirmation of relationshing having id ${event.partyRelationshipId} on queue",
          e
        )
      case Success(_) =>
        logger.info(s"Message has been sent on queue") // apz debug
    }

    DBIOAction.from(result)
  }
  def notifyInstitutionOnboarded(
    institutionId: UUID,
    productId: String,
    relationship: Relationship,
    queueEvent: QueueEvent
  )(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      institutionOpt <- institutionService.getInstitutionById(institutionId)
      institution    <- unpackInstitutionParty(institutionOpt)
      notification = InstitutionOnboardedNotificationObj.toNotification(
        institution,
        productId,
        relationship,
        queueEvent
      )
      _ <- datalakeContractsPublisher.send(notification)
    } yield ()
  }

  private def unpackInstitutionParty(institutionOpt: Option[Party]): Future[InstitutionParty] = {
    institutionOpt match {
      case Some(institutionParty: InstitutionParty) => Future.successful(institutionParty)
      case _                                        => Future.failed(GetInstitutionError)
    }
  }

  implicit val institutionOnboardedFormat: RootJsonFormat[InstitutionOnboarded] = jsonFormat7(InstitutionOnboarded)
  implicit val pspFormat: RootJsonFormat[PSP]                                   = jsonFormat10(PSP)
  implicit val institutionOnboardedBillingFormat: RootJsonFormat[InstitutionOnboardedBilling]           = jsonFormat3(
    InstitutionOnboardedBilling
  )
  implicit val institutionOnboardedNotificationFormat: RootJsonFormat[InstitutionOnboardedNotification] = jsonFormat16(
    InstitutionOnboardedNotification
  )
}
