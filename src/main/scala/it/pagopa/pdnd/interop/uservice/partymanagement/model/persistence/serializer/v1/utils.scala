package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1

import cats.implicits._
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.ErrorOr
import it.pagopa.pdnd.interop.commons.utils.TypeConversions._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.PersistedPartyRelationshipState._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.party.PartyV1.Empty
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.party.{
  InstitutionPartyV1,
  PartyV1,
  PersonPartyV1
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.relationship.PartyRelationshipV1
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.relationship.PartyRelationshipV1.{
  PartyRelationshipStateV1,
  PartyRoleV1
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
      for {
        start <- p.start.toOffsetDateTime.toEither
        end   <- p.end.traverse(_.toOffsetDateTime).toEither
      } yield PersonParty(id = UUID.fromString(p.id), start = start, end = end)

    case i: InstitutionPartyV1 =>
      for {
        start <- i.start.toOffsetDateTime.toEither
        end   <- i.end.traverse(_.toOffsetDateTime).toEither
      } yield InstitutionParty(
        id = UUID.fromString(i.id),
        externalId = i.externalId,
        description = i.description,
        digitalAddress = i.digitalAddress,
        taxCode = i.taxCode,
        attributes = i.attributes.toSet,
        products = i.products.toSet,
        start = start,
        end = end
      )
    case Empty => Left(new RuntimeException("Deserialization from protobuf failed"))
  }

  def getPartyV1(party: Party): ErrorOr[PartyV1] = party match {
    case p: PersonParty =>
      for {
        start <- p.start.asFormattedString.toEither
        end   <- p.end.traverse(_.asFormattedString).toEither
      } yield PersonPartyV1(id = p.id.toString, start = start, end = end)
    case i: InstitutionParty =>
      for {
        start <- i.start.asFormattedString.toEither
        end   <- i.end.traverse(_.asFormattedString).toEither
      } yield InstitutionPartyV1(
        id = i.id.toString,
        externalId = i.externalId,
        description = i.description,
        digitalAddress = i.digitalAddress,
        taxCode = i.taxCode,
        attributes = i.attributes.toSeq,
        start = start,
        end = end
      )
  }

  def stringToUUID(uuidStr: String): ErrorOr[UUID] =
    Try { UUID.fromString(uuidStr) }.toEither

  def getPartyRelationship(partyRelationshipV1: PartyRelationshipV1): ErrorOr[PersistedPartyRelationship] = {
    for {
      id        <- stringToUUID(partyRelationshipV1.id)
      from      <- stringToUUID(partyRelationshipV1.from)
      to        <- stringToUUID(partyRelationshipV1.to)
      partyRole <- partyRoleFromProtobuf(partyRelationshipV1.role)
      state     <- relationshipStateFromProtobuf(partyRelationshipV1.state)
      start     <- partyRelationshipV1.start.toOffsetDateTime.toEither
      end       <- partyRelationshipV1.end.traverse(_.toOffsetDateTime).toEither
    } yield PersistedPartyRelationship(
      id = id,
      from = from,
      to = to,
      role = partyRole,
      products = partyRelationshipV1.products.toSet,
      productRole = partyRelationshipV1.productRole,
      start = start,
      end = end,
      state = state,
      filePath = partyRelationshipV1.filePath,
      fileName = partyRelationshipV1.fileName,
      contentType = partyRelationshipV1.contentType
    )
  }

  def getPartyRelationshipV1(partyRelationship: PersistedPartyRelationship): ErrorOr[PartyRelationshipV1] = {
    for {
      start <- partyRelationship.start.asFormattedString.toEither
      end   <- partyRelationship.end.traverse(_.asFormattedString).toEither
    } yield PartyRelationshipV1(
      id = partyRelationship.id.toString,
      from = partyRelationship.from.toString,
      to = partyRelationship.to.toString,
      role = partyRoleToProtobuf(partyRelationship.role),
      productRole = partyRelationship.productRole,
      start = start,
      end = end,
      state = relationshipStateToProtobuf(partyRelationship.state),
      filePath = partyRelationship.filePath
    )
  }

  def getToken(tokenV1: TokenV1): ErrorOr[Token] = {
    for {
      legals   <- tokenV1.legals.traverse(partyRelationshipBindingMapper)
      validity <- tokenV1.validity.toOffsetDateTime.toEither
    } yield Token(
      id = tokenV1.id,
      legals = legals,
      validity = validity,
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
    token.validity.asFormattedString.toEither.map(validity =>
      TokenV1(
        id = token.id,
        legals =
          token.legals.map(legal => PartyRelationshipBindingV1(legal.partyId.toString, legal.relationshipId.toString)),
        validity = validity,
        seed = token.seed.toString,
        checksum = token.checksum
      )
    )

  }

  def partyRoleFromProtobuf(role: PartyRoleV1): ErrorOr[PersistedPartyRole] =
    role match {
      case PartyRoleV1.DELEGATE => Right(Manager)
      case PartyRoleV1.MANAGER  => Right(Delegate)
      case PartyRoleV1.OPERATOR => Right(Operator)
      case PartyRoleV1.Unrecognized(value) =>
        Left(new RuntimeException(s"Unable to deserialize party role value $value"))
    }

  def relationshipStateFromProtobuf(state: PartyRelationshipStateV1): ErrorOr[PersistedPartyRelationshipState] =
    state match {
      case PartyRelationshipStateV1.PENDING   => Right(Pending)
      case PartyRelationshipStateV1.ACTIVE    => Right(Active)
      case PartyRelationshipStateV1.SUSPENDED => Right(Suspended)
      case PartyRelationshipStateV1.DELETED   => Right(Deleted)
      case PartyRelationshipStateV1.REJECTED  => Right(Rejected)
      case PartyRelationshipStateV1.Unrecognized(value) =>
        Left(new RuntimeException(s"Unable to deserialize party relationship state value $value"))
    }

  def partyRoleToProtobuf(role: PersistedPartyRole): PartyRoleV1 =
    role match {
      case Manager  => PartyRoleV1.DELEGATE
      case Delegate => PartyRoleV1.MANAGER
      case Operator => PartyRoleV1.OPERATOR
    }

  def relationshipStateToProtobuf(state: PersistedPartyRelationshipState): PartyRelationshipStateV1 =
    state match {
      case Pending   => PartyRelationshipStateV1.PENDING
      case Active    => PartyRelationshipStateV1.ACTIVE
      case Suspended => PartyRelationshipStateV1.SUSPENDED
      case Deleted   => PartyRelationshipStateV1.DELETED
      case Rejected  => PartyRelationshipStateV1.REJECTED
    }

}
