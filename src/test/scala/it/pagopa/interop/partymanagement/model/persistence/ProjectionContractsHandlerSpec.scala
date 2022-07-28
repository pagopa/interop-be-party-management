//package it.pagopa.interop.partymanagement.model.persistence
//
//import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
//import akka.projection.eventsourced.EventEnvelope
//import com.typesafe.config.{Config, ConfigFactory}
//import it.pagopa.interop.commons.queue.kafka.KafkaPublisher
//import it.pagopa.interop.partymanagement.model.party.Token
//import org.scalamock.scalatest.MockFactory
//import org.scalatest.wordspec.AnyWordSpecLike
//import spray.json.JsonWriter
//import java.time.OffsetDateTime
//import java.util.UUID
//import scala.concurrent.Future
//
//object ProjectionContractsHandlerSpec {
//
//  val testData: Config = ConfigFactory.parseString(s"""
//      akka.actor.provider = cluster
//
//      akka.remote.classic.netty.tcp.port = 0
//      akka.remote.artery.canonical.port = 0
//      akka.remote.artery.canonical.hostname = 127.0.0.1
//
//      akka.cluster.jmx.multi-mbeans-in-same-jvm = on
//
//      akka.cluster.sharding.number-of-shards = 10
//
//      akka.coordinated-shutdown.terminate-actor-system = off
//      akka.coordinated-shutdown.run-by-actor-system-terminate = off
//      akka.coordinated-shutdown.run-by-jvm-shutdown-hook = off
//      akka.cluster.run-coordinated-shutdown-when-down = off
//    """)
//
//  val config: Config = ConfigFactory
//    .parseResourcesAnySyntax("application-test")
//    .withFallback(testData)
//    .resolve()
//}
//
//class ProjectionContractsHandlerSpec
//    extends ScalaTestWithActorTestKit(ProjectionContractsHandlerSpec.config)
//    with MockFactory
//    with AnyWordSpecLike {
//
//  val datalakeContractsPublisherMock: KafkaPublisher = mock[KafkaPublisher]
//  val projectionHandler: ProjectionContractsHandler  =
//    new ProjectionContractsHandler("tag", datalakeContractsPublisherMock)(system.executionContext)
//
//  "Projecting PartyRelationshipConfirmed event" should {
//    "publish on kafka" in {
//      val event: PartyRelationshipConfirmed = PartyRelationshipConfirmed(
//        UUID.randomUUID(),
//        "filePath",
//        "fileName",
//        "contentType",
//        UUID.randomUUID(),
//        OffsetDateTime.now
//      )
//
//      (datalakeContractsPublisherMock
//        .send(_: PartyRelationshipConfirmed)(_: JsonWriter[PartyRelationshipConfirmed]))
//        .expects(event, *)
//        .returning(Future.successful("OK"))
//        .once()
//
//      projectionHandler.process(new EventEnvelope(null, null, 0, event, 0))
//
//    }
//  }
//
//  "Projecting other events" should {
//    "not publish on kafka" in {
//      val event: TokenAdded = TokenAdded(new Token(UUID.randomUUID(), "checksum", Seq.empty, null, null))
//
//      (datalakeContractsPublisherMock
//        .send(_: TokenAdded)(_: JsonWriter[TokenAdded]))
//        .expects(*, *)
//        .never()
//
//      projectionHandler.process(new EventEnvelope(null, null, 0, event, 0))
//
//    }
//  }
//
//}
