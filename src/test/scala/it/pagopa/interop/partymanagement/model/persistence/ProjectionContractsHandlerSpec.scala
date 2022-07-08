package it.pagopa.interop.partymanagement.model.persistence

import akka.projection.eventsourced.EventEnvelope
import it.pagopa.interop.commons.queue.kafka.KafkaPublisher
import it.pagopa.interop.partymanagement.model.party.Token
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import spray.json.JsonWriter

import java.time.OffsetDateTime
import java.util.UUID

class ProjectionContractsHandlerSpec extends MockFactory with AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  val datalakeContractsPublisherMock: KafkaPublisher = mock[KafkaPublisher]
  val projectionHandler: ProjectionContractsHandler  =
    new ProjectionContractsHandler("tag", datalakeContractsPublisherMock)

  "Projecting PartyRelationshipConfirmed event" should {
    "publish on kafka" in {
      val event: PartyRelationshipConfirmed = PartyRelationshipConfirmed(
        UUID.randomUUID(),
        "filePath",
        "fileName",
        "contentType",
        UUID.randomUUID(),
        OffsetDateTime.now
      )

      (datalakeContractsPublisherMock
        .send(_: PartyRelationshipConfirmed)(_: JsonWriter[PartyRelationshipConfirmed]))
        .expects(event, *)
        .once()

      projectionHandler.process(new EventEnvelope(null, null, 0, event, 0))

    }
  }

  "Projecting other events" should {
    "not publish on kafka" in {
      val event: TokenAdded = TokenAdded(new Token(UUID.randomUUID(), "checksum", Seq.empty, null, null))

      (datalakeContractsPublisherMock
        .send(_: TokenAdded)(_: JsonWriter[TokenAdded]))
        .expects(*, *)
        .never()

      projectionHandler.process(new EventEnvelope(null, null, 0, event, 0))

    }
  }

}
