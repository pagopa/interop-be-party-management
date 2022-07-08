package it.pagopa.interop.partymanagement.model.persistence

import akka.Done
import akka.actor.typed.ActorSystem
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.ProjectionId
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.{ExactlyOnceProjection, SourceProvider}
import akka.projection.slick.{SlickHandler, SlickProjection}
import it.pagopa.interop.commons.queue.kafka.KafkaPublisher
import it.pagopa.interop.commons.utils.SprayCommonFormats.{offsetDateTimeFormat, uuidFormat}
import org.slf4j.{Logger, LoggerFactory}
import slick.basic.DatabaseConfig
import slick.dbio._
import slick.jdbc.JdbcProfile
import spray.json.DefaultJsonProtocol._

class PartyPersistentContractsProjection(
  system: ActorSystem[_],
  dbConfig: DatabaseConfig[JdbcProfile],
  datalakeContractsPublisher: KafkaPublisher
) {

  def sourceProvider(tag: String): SourceProvider[Offset, EventEnvelope[Event]] =
    EventSourcedProvider
      .eventsByTag[Event](system, readJournalPluginId = JdbcReadJournal.Identifier, tag = tag)

  def projection(tag: String): ExactlyOnceProjection[Offset, EventEnvelope[Event]] = {
    implicit val as: ActorSystem[_] = system
    SlickProjection.exactlyOnce(
      projectionId = ProjectionId("party-contracts-projections", tag),
      sourceProvider = sourceProvider(tag),
      handler = () => new ProjectionContractsHandler(tag, datalakeContractsPublisher),
      databaseConfig = dbConfig
    )
  }
}

class ProjectionContractsHandler(tag: String, datalakeContractsPublisher: KafkaPublisher)
    extends SlickHandler[EventEnvelope[Event]] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def process(envelope: EventEnvelope[Event]): DBIO[Done] = {
    envelope.event match {
      case event: PartyRelationshipConfirmed =>
        relationshipConfirmed(event)
      case _                                 =>
        logger.debug("This is the envelope event payload > {}", envelope.event)
        logger.debug("On tagged projection > {}", tag)
        DBIOAction.successful(Done)
    }
  }

  def relationshipConfirmed(event: PartyRelationshipConfirmed): DBIO[Done] = {
    logger.debug(s"projecting confirmation of relationship having id ${event.partyRelationshipId}")
    datalakeContractsPublisher.send(event)(jsonFormat6(PartyRelationshipConfirmed))
    DBIOAction.successful(Done)
  }
}
