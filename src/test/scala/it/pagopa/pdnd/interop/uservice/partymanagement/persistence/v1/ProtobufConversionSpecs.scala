package it.pagopa.pdnd.interop.uservice.partymanagement.persistence.v1
import it.pagopa.pdnd.interop.commons.utils.TypeConversions.OffsetDateTimeOps
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.ErrorOr
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{InstitutionParty, Party, PersonParty}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.party.{
  InstitutionPartyV1,
  PartyV1,
  PersonPartyV1
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.utils._
import org.scalatest.EitherValues._
import org.scalatest.TryValues._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
import java.util.UUID
import scala.util.Try

class ProtobufConversionSpecs extends AnyWordSpecLike with Matchers {

  "Protobuf conversions" should {

    "convert a PartyV1 (PersonPartyV1) to Party (PersonParty)" in {
      val id    = UUID.randomUUID()
      val start = OffsetDateTime.now()
      val end   = OffsetDateTime.now().plusDays(10L)
      val partyV1: Try[PersonPartyV1] =
        for {
          start <- start.asFormattedString
          end   <- end.asFormattedString
        } yield PersonPartyV1(id = id.toString, start = start, end = Some(end))

      val party: Either[Throwable, Party] = partyV1.toEither.flatMap(getParty)

      val expected: PersonParty = PersonParty(id = id, start = start, end = Some(end))

      party.value shouldBe expected

    }

    "convert a PartyV1 (InstitutionPartyV1) to Party (InstitutionParty)" in {
      val id             = UUID.randomUUID()
      val externalId     = "externalId"
      val description    = "description"
      val digitalAddress = "digitalAddress"
      val taxCode        = "taxCode"
      val start          = OffsetDateTime.now()
      val end            = OffsetDateTime.now().plusDays(10L)
      val attributes     = Set("a", "b")

      val partyV1: Try[InstitutionPartyV1] =
        for {
          start <- start.asFormattedString
          end   <- end.asFormattedString
        } yield InstitutionPartyV1(
          id = id.toString,
          externalId = externalId,
          description = description,
          digitalAddress = digitalAddress,
          taxCode = taxCode,
          start = start,
          end = Some(end),
          attributes = attributes.toSeq
        )

      val party: Either[Throwable, Party] = partyV1.toEither.flatMap(getParty)

      val expected: InstitutionParty = InstitutionParty(
        id = id,
        externalId = externalId,
        description = description,
        digitalAddress = digitalAddress,
        taxCode = taxCode,
        start = start,
        end = Some(end),
        attributes = attributes
      )

      party.value shouldBe expected

    }

    "convert a Party (PersonParty) to PartyV1 (PersonPartyV1)" in {
      val id                 = UUID.randomUUID()
      val start              = OffsetDateTime.now()
      val end                = OffsetDateTime.now().plusDays(10L)
      val party: PersonParty = PersonParty(id = id, start = start, end = Some(end))

      val partyV1: ErrorOr[PartyV1] = getPartyV1(party)

      val expected: Try[PersonPartyV1] =
        for {
          start <- start.asFormattedString
          end   <- end.asFormattedString
        } yield PersonPartyV1(id = id.toString, start = start, end = Some(end))

      partyV1.value shouldBe expected.success.value

    }

    "convert a Party (InstitutionParty) to PartyV1 (InstitutionPartyV1)" in {
      val id             = UUID.randomUUID()
      val externalId     = "externalId"
      val description    = "description"
      val digitalAddress = "digitalAddress"
      val taxCode        = "taxCode"
      val start          = OffsetDateTime.now()
      val end            = OffsetDateTime.now().plusDays(10L)
      val attributes     = Set("a", "b")

      val party: InstitutionParty = InstitutionParty(
        id = id,
        externalId = externalId,
        description = description,
        digitalAddress = digitalAddress,
        taxCode = taxCode,
        start = start,
        end = Some(end),
        attributes = attributes
      )

      val partyV1: Either[Throwable, PartyV1] = getPartyV1(party)

      val expected: Try[InstitutionPartyV1] =
        for {
          start <- start.asFormattedString
          end   <- end.asFormattedString
        } yield InstitutionPartyV1(
          id = id.toString,
          externalId = externalId,
          description = description,
          digitalAddress = digitalAddress,
          taxCode = taxCode,
          start = start,
          end = Some(end),
          attributes = attributes.toSeq
        )

      partyV1.value shouldBe expected.success.value

    }

  }

}
