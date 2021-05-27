package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v2

import cats.implicits._
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.{ErrorOr, formatter, toOffsetDateTime}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v2.party.PartyV2.Empty
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v2.party.{
  InstitutionPartyV2,
  PartyV2,
  PersonPartyV2
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v2.relationship.{
  PartyRelationShipIdV2,
  PartyRelationShipV2
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v2.relationship.PartyRelationShipIdV2.PartyRoleV2
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v2.relationship.PartyRelationShipV2.PartyRelationShipStatusV2
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v2.token.TokenV2
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v2.token.TokenV2.TokenStatusV2

import java.util.UUID

object utils {

  def getParty(partyV2: PartyV2): ErrorOr[Party] = partyV2 match {
    case p: PersonPartyV2 =>
      Right(
        PersonParty(
          id = UUID.fromString(p.id),
          externalId = p.externalId,
          name = p.name,
          surname = p.surname,
          start = toOffsetDateTime(p.start),
          end = p.end.map(toOffsetDateTime)
        )
      )
    case i: InstitutionPartyV2 =>
      Right(
        InstitutionParty(
          id = UUID.fromString(i.id),
          externalId = i.externalId,
          description = i.description,
          digitalAddress = i.digitalAddress,
          manager = i.manager,
          attributes = Set.empty,
          start = toOffsetDateTime(i.start),
          end = i.end.map(toOffsetDateTime)
        )
      )
    case Empty => Left(new RuntimeException("Deserialization from protobuf failed"))
  }

  def getPartyV2(party: Party): ErrorOr[PartyV2] = party match {
    case p: PersonParty =>
      Right(
        PersonPartyV2(
          id = p.id.toString,
          externalId = p.externalId,
          name = p.name,
          surname = p.surname,
          start = p.start.format(formatter),
          end = p.end.map(_.format(formatter))
        )
      )
    case i: InstitutionParty =>
      Right(
        InstitutionPartyV2(
          id = i.id.toString,
          externalId = i.externalId,
          description = i.description,
          digitalAddress = i.digitalAddress,
          manager = i.manager,
          start = i.start.format(formatter),
          end = i.end.map(_.format(formatter))
        )
      )
  }

  def getPartyRelationShipId(partyRelationShipIdV2: PartyRelationShipIdV2): ErrorOr[PartyRelationShipId] =
    PartyRole
      .fromText(partyRelationShipIdV2.status.name)
      .map(role =>
        PartyRelationShipId(
          UUID.fromString(partyRelationShipIdV2.from),
          UUID.fromString(partyRelationShipIdV2.to),
          role
        )
      )

  def getPartyRelationShipIdV2(partyRelationShipId: PartyRelationShipId): ErrorOr[PartyRelationShipIdV2] = {
    PartyRoleV2
      .fromName(partyRelationShipId.role.stringify)
      .toRight(new RuntimeException("Deserialization from protobuf failed"))
      .map(role => PartyRelationShipIdV2(partyRelationShipId.from.toString, partyRelationShipId.to.toString, role))
  }

  def getPartyRelationShip(partyRelationShipV2: PartyRelationShipV2): ErrorOr[PartyRelationShip] = {
    for {
      id     <- getPartyRelationShipId(partyRelationShipV2.id)
      status <- PartyRelationShipStatus.fromText(partyRelationShipV2.status.name)
    } yield PartyRelationShip(
      id = id,
      start = toOffsetDateTime(partyRelationShipV2.start),
      end = partyRelationShipV2.end.map(toOffsetDateTime),
      status = status
    )
  }

  def getPartyRelationShipV2(partyRelationShip: PartyRelationShip): ErrorOr[PartyRelationShipV2] = {
    for {
      id <- getPartyRelationShipIdV2(partyRelationShip.id)
      status <- PartyRelationShipStatusV2
        .fromName(partyRelationShip.status.stringify)
        .toRight(new RuntimeException("Deserialization from protobuf failed"))
    } yield PartyRelationShipV2(
      id = id,
      start = partyRelationShip.start.format(formatter),
      end = partyRelationShip.end.map(_.format(formatter)),
      status = status
    )

  }

  def getToken(tokenV2: TokenV2): ErrorOr[Token] = {
    for {
      legals <- tokenV2.legals.traverse(legal => getPartyRelationShipId(legal))
      status <- TokenStatus.fromText(tokenV2.status.name)
    } yield Token(
      id = tokenV2.id,
      legals = legals,
      validity = toOffsetDateTime(tokenV2.validity),
      status = status,
      seed = UUID.fromString(tokenV2.seed)
    )
  }

  def getTokenV2(token: Token): ErrorOr[TokenV2] = {
    for {
      legals <- token.legals.traverse(legal => getPartyRelationShipIdV2(legal))
      status <- TokenStatusV2
        .fromName(token.status.stringify)
        .toRight(new RuntimeException("Deserialization from protobuf failed"))
    } yield TokenV2(
      id = token.id,
      legals = legals,
      validity = token.validity.format(formatter),
      status = status,
      seed = token.seed.toString
    )
  }
}
