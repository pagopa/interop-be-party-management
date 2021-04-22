package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.{formatter, toOffsetDateTime}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.party.PartyV1.Empty
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.party.{
  InstitutionPartyV1,
  PartyV1,
  PersonPartyV1
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.relationship.{
  PartyRelationShipIdV1,
  PartyRelationShipStatusV1,
  PartyRelationShipV1,
  PartyRoleV1
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.state.{
  IndexesV1,
  PartiesV1,
  RelationShipsV1,
  TokensV1
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.token.{TokenStatusV1, TokenV1}

import java.util.UUID

package object v1 {

  // Party Ops
  implicit class PartyV1Ops(val partyV1: PartyV1) extends AnyVal {
    def toParty: Party = partyV1 match {
      case p: PersonPartyV1 =>
        PersonParty(
          id = UUID.fromString(p.id),
          externalId = p.externalId,
          name = p.name,
          surname = p.surname,
          start = toOffsetDateTime(p.start),
          end = p.end.map(toOffsetDateTime)
        )
      case i: InstitutionPartyV1 =>
        InstitutionParty(
          id = UUID.fromString(i.id),
          externalId = i.externalId,
          description = i.description,
          digitalAddress = i.digitalAddress,
          manager = i.manager,
          start = toOffsetDateTime(i.start),
          end = i.end.map(toOffsetDateTime)
        )
      case Empty => ???
    }
  }

  implicit class PartyOps(val party: Party) extends AnyVal {
    def toPartyV1: PartyV1 = party match {
      case p: PersonParty =>
        PersonPartyV1(
          id = p.id.toString,
          externalId = p.externalId,
          name = p.name,
          surname = p.surname,
          start = p.start.format(formatter),
          end = p.end.map(_.format(formatter))
        )
      case i: InstitutionParty =>
        InstitutionPartyV1(
          id = i.id.toString,
          externalId = i.externalId,
          description = i.description,
          digitalAddress = i.digitalAddress,
          manager = i.manager,
          start = i.start.format(formatter),
          end = i.end.map(_.format(formatter))
        )
    }
  }

  // PartyRelationShipId* Ops
  implicit class PartyRelationShipIdOps(val partyRelationShipId: PartyRelationShipId) extends AnyVal {
    def toPartyRelationShipIdV1: PartyRelationShipIdV1 = PartyRelationShipIdV1(
      partyRelationShipId.from.toString,
      partyRelationShipId.to.toString,
      PartyRoleV1.fromName(partyRelationShipId.role.stringify).get //TODO too weak
    )
  }

  implicit class PartyRelationShipIdV1Ops(val partyRelationShipIdV1: PartyRelationShipIdV1) extends AnyVal {
    def toPartyRelationShipId: PartyRelationShipId = PartyRelationShipId(
      UUID.fromString(partyRelationShipIdV1.from),
      UUID.fromString(partyRelationShipIdV1.to),
      PartyRole(partyRelationShipIdV1.status.name)
    )
  }

  // PartyRelationShip* Ops
  implicit class PartyRelationShipV1Ops(val partyRelationShipV1: PartyRelationShipV1) extends AnyVal {
    def toPartyRelationShip: PartyRelationShip =
      PartyRelationShip(
        id = partyRelationShipV1.id.toPartyRelationShipId,
        start = toOffsetDateTime(partyRelationShipV1.start),
        end = partyRelationShipV1.end.map(toOffsetDateTime),
        status = PartyRelationShipStatus(partyRelationShipV1.status.name)
      )
  }

  implicit class PartyRelationShipOps(val partyRelationShip: PartyRelationShip) extends AnyVal {
    def toPartyRelationShipV1: PartyRelationShipV1 =
      PartyRelationShipV1(
        id = partyRelationShip.id.toPartyRelationShipIdV1,
        start = partyRelationShip.start.format(formatter),
        end = partyRelationShip.end.map(_.format(formatter)),
        status = PartyRelationShipStatusV1.fromName(partyRelationShip.status.stringify).get
      )
  }

  // Token* Ops
  implicit class TokenOps(val token: Token) extends AnyVal {
    def toTokenV1: TokenV1 = TokenV1(
      manager = token.manager.toPartyRelationShipIdV1,
      delegate = token.delegate.toPartyRelationShipIdV1,
      validity = token.validity.format(formatter),
      status = TokenStatusV1.fromName(token.status.stringify).get, //TODO too weak
      seed = token.seed.toString
    )
  }

  implicit class TokenV1Ops(val tokenV1: TokenV1) extends AnyVal {
    def toToken: Token = Token(
      manager = tokenV1.manager.toPartyRelationShipId,
      delegate = tokenV1.delegate.toPartyRelationShipId,
      validity = toOffsetDateTime(tokenV1.validity),
      status = TokenStatus(tokenV1.status.name),
      seed = UUID.fromString(tokenV1.seed)
    )
  }

  // State

  implicit class PartiesV1Ops(val partiesV1: Seq[PartiesV1]) extends AnyVal {
    def toParties: Map[UUID, Party] = partiesV1.map(p => UUID.fromString(p.key) -> p.value.toParty).toMap
  }

  implicit class IndexesV1Ops(val indexesV1: Seq[IndexesV1]) extends AnyVal {
    def toIndexes: Map[String, UUID] = indexesV1.map(p => p.key -> UUID.fromString(p.value)).toMap
  }

  implicit class TokensV1Ops(val tokensV1: Seq[TokensV1]) extends AnyVal {
    def toTokens: Map[UUID, Token] = tokensV1.map(p => UUID.fromString(p.key) -> p.value.toToken).toMap
  }

  implicit class RelationShipsV1Ops(val relationShipsV1: Seq[RelationShipsV1]) extends AnyVal {
    def toRelationShips: Map[PartyRelationShipId, PartyRelationShip] =
      relationShipsV1.map(p => p.key.toPartyRelationShipId -> p.value.toPartyRelationShip).toMap
  }

}
