package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import cats.implicits._
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.ErrorOr
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{
  Party,
  PartyRelationship,
  PartyRelationshipId,
  Token
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.events._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.state._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.utils.{
  getParty,
  getPartyRelationship,
  getPartyRelationshipId,
  getPartyRelationshipIdV1,
  getPartyRelationshipV1,
  getToken,
  _
}

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
          .traverse[ErrorOr, (PartyRelationshipId, PartyRelationship)](rl =>
            for {
              k <- getPartyRelationshipId(rl.key)
              v <- getPartyRelationship(rl.value)
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
          .traverse[ErrorOr, RelationshipsV1] { case (key, value) =>
            for {
              k <- getPartyRelationshipIdV1(key)
              v <- getPartyRelationshipV1(value)
            } yield RelationshipsV1(k, v)
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

  implicit def attributesAddedV1PersistEventDeserializer: PersistEventDeserializer[AttributesAddedV1, AttributesAdded] =
    event => getParty(event.party).map(AttributesAdded)

  implicit def attributesAddedV1PersistEventSerializer: PersistEventSerializer[AttributesAdded, AttributesAddedV1] =
    event => getPartyV1(event.party).map(AttributesAddedV1.of)

  implicit def partyRelationshipAddedV1PersistEventDeserializer
    : PersistEventDeserializer[PartyRelationshipAddedV1, PartyRelationshipAdded] = event =>
    getPartyRelationship(event.partyRelationship).map(PartyRelationshipAdded)

  implicit def partyRelationshipConfirmedV1PersistEventSerializer
    : PersistEventSerializer[PartyRelationshipConfirmed, PartyRelationshipConfirmedV1] =
    event => getPartyRelationshipIdV1(event.partyRelationshipId).map(PartyRelationshipConfirmedV1.of)

  implicit def partyRelationshipConfirmedV1PersistEventDeserializer
    : PersistEventDeserializer[PartyRelationshipConfirmedV1, PartyRelationshipConfirmed] = event =>
    getPartyRelationshipId(event.partyRelationshipId).map(PartyRelationshipConfirmed)

  implicit def partyRelationshipAddedV1PersistEventSerializer
    : PersistEventSerializer[PartyRelationshipAdded, PartyRelationshipAddedV1] =
    event => getPartyRelationshipV1(event.partyRelationship).map(PartyRelationshipAddedV1.of)

  implicit def partyRelationshipDeletedV1PersistEventDeserializer
    : PersistEventDeserializer[PartyRelationshipDeletedV1, PartyRelationshipDeleted] = event =>
    getPartyRelationshipId(event.partyRelationshipId).map(PartyRelationshipDeleted)

  implicit def partyRelationshipDeletedV1PersistEventSerializer
    : PersistEventSerializer[PartyRelationshipDeleted, PartyRelationshipDeletedV1] =
    event => getPartyRelationshipIdV1(event.partyRelationshipId).map(PartyRelationshipDeletedV1.of)

  implicit def tokenAddedV1PersistEventDeserializer: PersistEventDeserializer[TokenAddedV1, TokenAdded] = event =>
    getToken(event.token).map(TokenAdded)

  implicit def tokenAddedV1PersistEventSerializer: PersistEventSerializer[TokenAdded, TokenAddedV1] =
    event => getTokenV1(event.token).map(TokenAddedV1.of)

  implicit def tokenDeletedV1PersistEventDeserializer: PersistEventDeserializer[TokenDeletedV1, TokenDeleted] = event =>
    getToken(event.token).map(TokenDeleted)

  implicit def tokenDeletedV1PersistEventSerializer: PersistEventSerializer[TokenDeleted, TokenDeletedV1] =
    event => getTokenV1(event.token).map(TokenDeletedV1.of)

}
