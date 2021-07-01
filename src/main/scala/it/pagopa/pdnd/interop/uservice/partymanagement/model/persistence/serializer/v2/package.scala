package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import cats.implicits._
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.ErrorOr
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{
  Party,
  PartyRelationShip,
  PartyRelationShipId,
  Token
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v2.utils.{
  getPartyRelationShip,
  getPartyRelationShipId,
  getPartyRelationShipIdV2,
  getPartyRelationShipV2
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v2.events._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v2.state._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v2.utils.{getParty, getToken, _}

import java.util.UUID

package object v2 {
  @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
  implicit def stateV2PersistEventDeserializer: PersistEventDeserializer[StateV2, State] =
    state =>
      for {
        parties <- state.parties
          .traverse[ErrorOr, (UUID, Party)](ps => getParty(ps.value).map(p => UUID.fromString(ps.key) -> p))
          .map(_.toMap)
        indexes <- Right(state.indexes.map(p => p.key -> UUID.fromString(p.value)).toMap)
        tokens <- state.tokens
          .traverse[ErrorOr, (String, Token)](ts => getToken(ts.value).map(t => ts.key -> t))
          .map(_.toMap)
        relationShips <- state.relationShips
          .traverse[ErrorOr, (PartyRelationShipId, PartyRelationShip)](rl =>
            for {
              k <- getPartyRelationShipId(rl.key)
              v <- getPartyRelationShip(rl.value)
            } yield k -> v
          )
          .map(_.toMap)
      } yield State(parties, indexes, tokens, relationShips)

  @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
  implicit def stateV2PersistEventSerializer: PersistEventSerializer[State, StateV2] =
    state =>
      for {
        parties <- state.parties.toSeq.traverse[ErrorOr, PartiesV2] { case (k, v) =>
          getPartyV2(v).map(party => PartiesV2(k.toString, party))
        }
        indexes <- Right(state.indexes.map { case (k, v) => IndexesV2(k, v.toString) }.toSeq)
        tokens <- state.tokens.toSeq.traverse[ErrorOr, TokensV2] { case (k, v) =>
          getTokenV2(v).map(token => TokensV2(k, token))
        }
        relationShips <- state.relationShips.toSeq
          .traverse[ErrorOr, RelationShipsV2] { case (key, value) =>
            for {
              k <- getPartyRelationShipIdV2(key)
              v <- getPartyRelationShipV2(value)
            } yield RelationShipsV2(k, v)
          }
      } yield StateV2(parties, indexes, tokens, relationShips)

  implicit def partyAddedV2PersistEventDeserializer: PersistEventDeserializer[PartyAddedV2, PartyAdded] = event =>
    getParty(event.party).map(PartyAdded)

  implicit def partyAddedV2PersistEventSerializer: PersistEventSerializer[PartyAdded, PartyAddedV2] = event =>
    getPartyV2(event.party).map(PartyAddedV2.of)

  implicit def partyDeletedV2PersistEventDeserializer: PersistEventDeserializer[PartyDeletedV2, PartyDeleted] =
    event => getParty(event.party).map(PartyDeleted)

  implicit def partyDeletedV2PersistEventSerializer: PersistEventSerializer[PartyDeleted, PartyDeletedV2] =
    event => getPartyV2(event.party).map(PartyDeletedV2.of)

  implicit def partyRelationShipAddedV2PersistEventDeserializer
    : PersistEventDeserializer[PartyRelationShipAddedV2, PartyRelationShipAdded] = event =>
    getPartyRelationShip(event.partyRelationShip).map(PartyRelationShipAdded)

  implicit def partyRelationShipAddedV2PersistEventSerializer
    : PersistEventSerializer[PartyRelationShipAdded, PartyRelationShipAddedV2] =
    event => getPartyRelationShipV2(event.partyRelationShip).map(PartyRelationShipAddedV2.of)

  implicit def partyRelationShipDeletedV2PersistEventDeserializer
    : PersistEventDeserializer[PartyRelationShipDeletedV2, PartyRelationShipDeleted] = event =>
    getPartyRelationShipId(event.partyRelationShipId).map(PartyRelationShipDeleted)

  implicit def partyRelationShipDeletedV2PersistEventSerializer
    : PersistEventSerializer[PartyRelationShipDeleted, PartyRelationShipDeletedV2] =
    event => getPartyRelationShipIdV2(event.relationShipId).map(PartyRelationShipDeletedV2.of)

  implicit def tokenAddedV2PersistEventDeserializer: PersistEventDeserializer[TokenAddedV2, TokenAdded] = event =>
    getToken(event.token).map(TokenAdded)

  implicit def tokenAddedV2PersistEventSerializer: PersistEventSerializer[TokenAdded, TokenAddedV2] =
    event => getTokenV2(event.token).map(TokenAddedV2.of)

  implicit def tokenConsumedV2PersistEventDeserializer: PersistEventDeserializer[TokenConsumedV2, TokenConsumed] =
    event => getToken(event.token).map(TokenConsumed)

  implicit def tokenConsumedV2PersistEventSerializer: PersistEventSerializer[TokenConsumed, TokenConsumedV2] =
    event => getTokenV2(event.token).map(TokenConsumedV2.of)

  implicit def tokenInvalidatedV2PersistEventDeserializer
    : PersistEventDeserializer[TokenInvalidatedV2, TokenInvalidated] =
    event => getToken(event.token).map(TokenInvalidated)

  implicit def tokenInvalidatedV2PersistEventSerializer: PersistEventSerializer[TokenInvalidated, TokenInvalidatedV2] =
    event => getTokenV2(event.token).map(TokenInvalidatedV2.of)
}
