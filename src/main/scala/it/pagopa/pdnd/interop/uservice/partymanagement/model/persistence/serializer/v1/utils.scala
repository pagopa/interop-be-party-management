package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1

import cats.implicits._
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.{ErrorOr, formatter, toOffsetDateTime}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.party.PartyV1.Empty
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.party.{
  InstitutionPartyV1,
  PartyV1,
  PersonPartyV1
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.relationship.PartyRelationshipV1.{
  PartyRelationshipStatusV1,
  PartyRoleV1
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.relationship.PartyRelationshipV1
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.token.{
  PartyRelationshipBindingV1,
  TokenV1
}

import java.util.UUID
import scala.util.Try

@SuppressWarnings(Array("org.wartremover.warts.Nothing"))
object utils {

  def getParty(partyV1: PartyV1): ErrorOr[Party] = partyV1 match {
    case p: PersonPartyV1 =>
      Right(
        PersonParty(
          id = UUID.fromString(p.id),
          start = toOffsetDateTime(p.start),
          end = p.end.map(toOffsetDateTime)
        )
      )
    case i: InstitutionPartyV1 =>
      Right(
        InstitutionParty(
          id = UUID.fromString(i.id),
          externalId = i.externalId,
          description = i.description,
          digitalAddress = i.digitalAddress,
          attributes = i.attributes.toSet,
          start = toOffsetDateTime(i.start),
          end = i.end.map(toOffsetDateTime)
        )
      )
    case Empty => Left(new RuntimeException("Deserialization from protobuf failed"))
  }

  def getPartyV1(party: Party): ErrorOr[PartyV1] = party match {
    case p: PersonParty =>
      Right(
        PersonPartyV1(
          id = p.id.toString,
          start = p.start.format(formatter),
          end = p.end.map(_.format(formatter))
        )
      )
    case i: InstitutionParty =>
      Right(
        InstitutionPartyV1(
          id = i.id.toString,
          externalId = i.externalId,
          description = i.description,
          digitalAddress = i.digitalAddress,
          attributes = i.attributes.toSeq,
          start = i.start.format(formatter),
          end = i.end.map(_.format(formatter))
        )
      )
  }

  def stringToUUID(uuidStr: String): ErrorOr[UUID] =
    Try { UUID.fromString(uuidStr) }.toEither

  def getPartyRelationship(partyRelationshipV1: PartyRelationshipV1): ErrorOr[PartyRelationship] = {
    for {
      id        <- stringToUUID(partyRelationshipV1.id)
      from      <- stringToUUID(partyRelationshipV1.from)
      to        <- stringToUUID(partyRelationshipV1.to)
      partyRole <- PartyRole.fromText(partyRelationshipV1.role.name)
      status    <- PartyRelationshipStatus.fromText(partyRelationshipV1.status.name)
    } yield PartyRelationship(
      id = id,
      from = from,
      to = to,
      role = partyRole,
      platformRole = partyRelationshipV1.platformRole,
      start = toOffsetDateTime(partyRelationshipV1.start),
      end = partyRelationshipV1.end.map(toOffsetDateTime),
      status = status,
      filePath = partyRelationshipV1.filePath,
      fileName = partyRelationshipV1.fileName,
      contentType = partyRelationshipV1.contentType
    )
  }

  def getPartyRelationshipV1(partyRelationship: PartyRelationship): ErrorOr[PartyRelationshipV1] = {
    for {
      status <- PartyRelationshipStatusV1
        .fromName(partyRelationship.status.stringify)
        .toRight(new RuntimeException("Deserialization from protobuf failed"))
      partyRole <- PartyRoleV1
        .fromName(partyRelationship.role.stringify)
        .toRight(new RuntimeException("Deserialization from protobuf failed"))
    } yield PartyRelationshipV1(
      id = partyRelationship.id.toString,
      from = partyRelationship.from.toString,
      to = partyRelationship.to.toString,
      role = partyRole,
      platformRole = partyRelationship.platformRole,
      start = partyRelationship.start.format(formatter),
      end = partyRelationship.end.map(_.format(formatter)),
      status = status,
      filePath = partyRelationship.filePath
    )

  }

  def getToken(tokenV1: TokenV1): ErrorOr[Token] = {
    for {
      legals <- tokenV1.legals.traverse(partyRelationshipBindingMapper)
    } yield Token(
      id = tokenV1.id,
      legals = legals,
      validity = toOffsetDateTime(tokenV1.validity),
      seed = UUID.fromString(tokenV1.seed),
      checksum = tokenV1.checksum
    )
  }

  def partyRelationshipBindingMapper(
    partyRelationshipBindingV1: PartyRelationshipBindingV1
  ): ErrorOr[PartyRelationshipBinding] = {
    for {
      partyId        <- stringToUUID(partyRelationshipBindingV1.partyId)
      relationshipId <- stringToUUID(partyRelationshipBindingV1.relationshipId)
    } yield PartyRelationshipBinding(partyId, relationshipId)
  }

  def getTokenV1(token: Token): ErrorOr[TokenV1] = {
    Right[Throwable, TokenV1](
      TokenV1(
        id = token.id,
        legals =
          token.legals.map(legal => PartyRelationshipBindingV1(legal.partyId.toString, legal.relationshipId.toString)),
        validity = token.validity.format(formatter),
        seed = token.seed.toString,
        checksum = token.checksum
      )
    )
  }
}
