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
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.events._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.state._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.utils._

import java.util.UUID

package object v1 {
  @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
  implicit def stateV1PersistEventDeserializer: PersistEventDeserializer[StateV1, State] =
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
  implicit def stateV1PersistEventSerializer: PersistEventSerializer[State, StateV1] =
    state =>
      for {
        parties <- state.parties.toSeq.traverse[ErrorOr, PartiesV1] { case (k, v) =>
          getPartyV1(v).map(party => PartiesV1(k.toString, party))
        }
        indexes <- Right(state.indexes.map { case (k, v) => IndexesV1(k, v.toString) }.toSeq)
        tokens <- state.tokens.toSeq.traverse[ErrorOr, TokensV1] { case (k, v) =>
          getTokenV1(v).map(token => TokensV1(k, token))
        }
        relationShips <- state.relationShips.toSeq
          .traverse[ErrorOr, RelationShipsV1] { case (key, value) =>
            for {
              k <- getPartyRelationShipIdV1(key)
              v <- getPartyRelationShipV1(value)
            } yield RelationShipsV1(k, v)
          }
      } yield StateV1(parties, indexes, tokens, relationShips)

  implicit def partyAddedV1PersistEventDeserializer: PersistEventDeserializer[PartyAddedV1, PartyAdded] = event =>
    getParty(event.party).map(PartyAdded)

  implicit def partyAddedV1PersistEventSerializer: PersistEventSerializer[PartyAdded, PartyAddedV1] = event =>
    getPartyV1(event.party).map(PartyAddedV1.of)

  implicit def partyDeletedV1PersistEventDeserializer: PersistEventDeserializer[PartyDeletedV1, PartyDeleted] =
    event => getParty(event.party).map(PartyDeleted)

  implicit def partyDeletedV1PersistEventSerializer: PersistEventSerializer[PartyDeleted, PartyDeletedV1] =
    event => getPartyV1(event.party).map(PartyDeletedV1.of)

  implicit def partyRelationShipAddedV1PersistEventDeserializer
    : PersistEventDeserializer[PartyRelationShipAddedV1, PartyRelationShipAdded] = event =>
    getPartyRelationShip(event.partyRelationShip).map(PartyRelationShipAdded)

  implicit def partyRelationShipAddedV1PersistEventSerializer
    : PersistEventSerializer[PartyRelationShipAdded, PartyRelationShipAddedV1] =
    event => getPartyRelationShipV1(event.partyRelationShip).map(PartyRelationShipAddedV1.of)

  implicit def partyRelationShipDeletedV1PersistEventDeserializer
    : PersistEventDeserializer[PartyRelationShipDeletedV1, PartyRelationShipDeleted] = event =>
    getPartyRelationShipId(event.partyRelationShipId).map(PartyRelationShipDeleted)

  implicit def partyRelationShipDeletedV1PersistEventSerializer
    : PersistEventSerializer[PartyRelationShipDeleted, PartyRelationShipDeletedV1] =
    event => getPartyRelationShipIdV1(event.relationShipId).map(PartyRelationShipDeletedV1.of)

  implicit def tokenAddedV1PersistEventDeserializer: PersistEventDeserializer[TokenAddedV1, TokenAdded] = event =>
    getToken(event.token).map(TokenAdded)

  implicit def tokenAddedV1PersistEventSerializer: PersistEventSerializer[TokenAdded, TokenAddedV1] =
    event => getTokenV1(event.token).map(TokenAddedV1.of)

  implicit def tokenConsumedV1PersistEventDeserializer: PersistEventDeserializer[TokenConsumedV1, TokenConsumed] =
    event => getToken(event.token).map(TokenConsumed)

  implicit def tokenConsumedV1PersistEventSerializer: PersistEventSerializer[TokenConsumed, TokenConsumedV1] =
    event => getTokenV1(event.token).map(TokenConsumedV1.of)

  implicit def tokenInvalidatedV1PersistEventDeserializer
    : PersistEventDeserializer[TokenInvalidatedV1, TokenInvalidated] =
    event => getToken(event.token).map(TokenInvalidated)

  implicit def tokenInvalidatedV1PersistEventSerializer: PersistEventSerializer[TokenInvalidated, TokenInvalidatedV1] =
    event => getTokenV1(event.token).map(TokenInvalidatedV1.of)
}
