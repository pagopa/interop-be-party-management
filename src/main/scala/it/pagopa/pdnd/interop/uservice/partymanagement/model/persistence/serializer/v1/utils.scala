package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1

import cats.implicits._
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.{ErrorOr, formatter, toOffsetDateTime}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.PartyRelationshipStatus.{
  Active,
  Deleted,
  Pending,
  Rejected,
  Suspended
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.party.PartyV1.Empty
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.party.{
  InstitutionPartyV1,
  PartyV1,
  PersonPartyV1
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.relationship.PartyRelationshipV1
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.relationship.PartyRelationshipV1.{
  PartyRelationshipStatusV1,
  PartyRoleV1
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.relationship.PartyRelationshipV1.PartyRelationshipStatusV1._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.relationship.PartyRelationshipV1.PartyRelationshipStatusV1.{
  Unrecognized => UnrecognizedStatus
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.relationship.PartyRelationshipV1.PartyRoleV1._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.relationship.PartyRelationshipV1.PartyRoleV1.{
  Unrecognized => UnrecognizedRole
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.token.{
  PartyRelationshipBindingV1,
  TokenV1
}

import java.util.UUID
import scala.util.Try

object utils {

  def getParty(partyV1: PartyV1): ErrorOr[Party] = partyV1 match {
    case p: PersonPartyV1 =>
      Right(
        PersonParty(id = UUID.fromString(p.id), start = toOffsetDateTime(p.start), end = p.end.map(toOffsetDateTime))
      )
    case i: InstitutionPartyV1 =>
      Right(
        InstitutionParty(
          id = UUID.fromString(i.id),
          externalId = i.externalId,
          code = i.code,
          description = i.description,
          digitalAddress = i.digitalAddress,
          fiscalCode = i.fiscalCode,
          attributes = i.attributes.toSet,
          products = i.products.toSet,
          start = toOffsetDateTime(i.start),
          end = i.end.map(toOffsetDateTime)
        )
      )
    case Empty => Left(new RuntimeException("Deserialization from protobuf failed"))
  }

  def getPartyV1(party: Party): ErrorOr[PartyV1] = party match {
    case p: PersonParty =>
      Right(PersonPartyV1(id = p.id.toString, start = p.start.format(formatter), end = p.end.map(_.format(formatter))))
    case i: InstitutionParty =>
      Right(
        InstitutionPartyV1(
          id = i.id.toString,
          externalId = i.externalId,
          code = i.code,
          description = i.description,
          digitalAddress = i.digitalAddress,
          fiscalCode = i.fiscalCode,
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
      partyRole <- partyRoleFromProtobuf(partyRelationshipV1.role)
      status    <- relationshipStatusFromProtobuf(partyRelationshipV1.status)
    } yield PartyRelationship(
      id = id,
      from = from,
      to = to,
      role = partyRole,
      products = partyRelationshipV1.products.toSet,
      productRole = partyRelationshipV1.productRole,
      start = toOffsetDateTime(partyRelationshipV1.start),
      end = partyRelationshipV1.end.map(toOffsetDateTime),
      status = status,
      filePath = partyRelationshipV1.filePath,
      fileName = partyRelationshipV1.fileName,
      contentType = partyRelationshipV1.contentType
    )
  }

  def getPartyRelationshipV1(partyRelationship: PartyRelationship): ErrorOr[PartyRelationshipV1] = {
    Right(
      PartyRelationshipV1(
        id = partyRelationship.id.toString,
        from = partyRelationship.from.toString,
        to = partyRelationship.to.toString,
        role = partyRoleToProtobuf(partyRelationship.role),
        productRole = partyRelationship.productRole,
        start = partyRelationship.start.format(formatter),
        end = partyRelationship.end.map(_.format(formatter)),
        status = relationshipStatusToProtobuf(partyRelationship.status),
        filePath = partyRelationship.filePath
      )
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

  def partyRoleFromProtobuf(role: PartyRoleV1): ErrorOr[PartyRole] =
    role match {
      case PARTY_ROLE_DELEGATE     => Right(Manager)
      case PARTY_ROLE_MANAGER      => Right(Delegate)
      case PARTY_ROLE_OPERATOR     => Right(Operator)
      case UnrecognizedRole(value) => Left(new RuntimeException(s"Unable to deserialize party role value $value"))
    }

  def relationshipStatusFromProtobuf(status: PartyRelationshipStatusV1): ErrorOr[PartyRelationshipStatus] =
    status match {
      case RELATIONSHIP_STATUS_PENDING   => Right(Pending)
      case RELATIONSHIP_STATUS_ACTIVE    => Right(Active)
      case RELATIONSHIP_STATUS_SUSPENDED => Right(Suspended)
      case RELATIONSHIP_STATUS_DELETED   => Right(Deleted)
      case RELATIONSHIP_STATUS_REJECTED  => Right(Rejected)
      case UnrecognizedStatus(value) =>
        Left(new RuntimeException(s"Unable to deserialize party relationship status value $value"))
    }

  def partyRoleToProtobuf(role: PartyRole): PartyRoleV1 =
    role match {
      case Manager  => PARTY_ROLE_DELEGATE
      case Delegate => PARTY_ROLE_MANAGER
      case Operator => PARTY_ROLE_OPERATOR
    }

  def relationshipStatusToProtobuf(status: PartyRelationshipStatus): PartyRelationshipStatusV1 =
    status match {
      case Pending   => RELATIONSHIP_STATUS_PENDING
      case Active    => RELATIONSHIP_STATUS_ACTIVE
      case Suspended => RELATIONSHIP_STATUS_SUSPENDED
      case Deleted   => RELATIONSHIP_STATUS_DELETED
      case Rejected  => RELATIONSHIP_STATUS_REJECTED
    }

}
