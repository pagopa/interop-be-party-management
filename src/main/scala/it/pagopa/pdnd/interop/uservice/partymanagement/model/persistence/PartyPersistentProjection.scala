package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.{AtLeastOnceFlowProjection, SourceProvider}
import akka.projection.slick.SlickProjection
import akka.projection.{ProjectionContext, ProjectionId}
import akka.stream.scaladsl.FlowWithContext
import akka.{Done, NotUsed}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.DurationInt

final case class PartyPersistentProjection(
  system: ActorSystem[_],
  entity: Entity[Command, ShardingEnvelope[Command]],
  dbConfig: DatabaseConfig[JdbcProfile]
) {

  private val settings: ClusterShardingSettings = entity.settings match {
    case None    => ClusterShardingSettings(system)
    case Some(s) => s
  }

  def sourceProvider(tag: String): SourceProvider[Offset, EventEnvelope[Event]] =
    EventSourcedProvider
      .eventsByTag[Event](system, readJournalPluginId = JdbcReadJournal.Identifier, tag = tag)

  val flow
    : FlowWithContext[EventEnvelope[Event], ProjectionContext, EventEnvelope[Event], ProjectionContext, NotUsed]#Repr[
      Event,
      ProjectionContext
    ]#Repr[Done.type, ProjectionContext] = FlowWithContext[EventEnvelope[Event], ProjectionContext]
    .map(envelope => envelope.event)
    .map(event => {
      println(event)
      Done
    })

  def projection(tag: String): AtLeastOnceFlowProjection[Offset, EventEnvelope[Event]] = {
    implicit val as: ActorSystem[_] = system
    SlickProjection
      .atLeastOnceFlow(
        projectionId = ProjectionId("party-projections", tag),
        sourceProvider = sourceProvider(tag),
        handler = flow,
        databaseConfig = dbConfig
      )
      .withRestartBackoff(minBackoff = 10.seconds, maxBackoff = 60.seconds, randomFactor = 0.5)
  }

  val projections: Seq[AtLeastOnceFlowProjection[Offset, EventEnvelope[Event]]] =
    (0 until settings.numberOfShards).map(i =>
      projection(s"pdnd-interop-uservice-party-management-persistence-party|${i.toString}")
    )

}
