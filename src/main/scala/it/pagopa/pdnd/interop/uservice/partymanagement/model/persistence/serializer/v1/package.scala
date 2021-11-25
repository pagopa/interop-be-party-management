package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import cats.implicits._
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.ErrorOr
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{Party, PersistedPartyRelationship, Token}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.events._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.state._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.utils._

import java.util.UUID

package object v1 {

  implicit def stateV1PersistEventDeserializer: PersistEventDeserializer[StateV1, State] =
    state =>
      for {
        parties <- state.parties
          .traverse[ErrorOr, (UUID, Party)](ps => getParty(ps.value).map(p => UUID.fromString(ps.key) -> p))
          .map(_.toMap)
        tokens <- state.tokens
          .traverse[ErrorOr, (String, Token)](ts => getToken(ts.value).map(t => ts.key -> t))
          .map(_.toMap)
        relationships <- state.relationships
          .traverse[ErrorOr, (String, PersistedPartyRelationship)](rl =>
            for {
              v <- getPartyRelationship(rl.value)
            } yield rl.key -> v
          )
          .map(_.toMap)
      } yield State(parties, tokens, relationships)

  implicit def stateV1PersistEventSerializer: PersistEventSerializer[State, StateV1] =
    state =>
      for {
        parties <- state.parties.toSeq.traverse[ErrorOr, PartiesV1] { case (k, v) =>
          getPartyV1(v).map(party => PartiesV1(k.toString, party))
        }
        tokens <- state.tokens.toSeq.traverse[ErrorOr, TokensV1] { case (k, v) =>
          getTokenV1(v).map(token => TokensV1(k, token))
        }
        relationships <- state.relationships.toSeq
          .traverse[ErrorOr, RelationshipEntryV1] { case (key, value) =>
            for {
              v <- getPartyRelationshipV1(value)
            } yield RelationshipEntryV1(key, v)
          }
      } yield StateV1(parties, tokens, relationships)

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
    event =>
      Right[Throwable, PartyRelationshipConfirmedV1](
        PartyRelationshipConfirmedV1
          .of(
            partyRelationshipId = event.partyRelationshipId.toString,
            filePath = event.filePath,
            fileName = event.fileName,
            contentType = event.contentType
          )
      )

  implicit def partyRelationshipConfirmedV1PersistEventDeserializer
    : PersistEventDeserializer[PartyRelationshipConfirmedV1, PartyRelationshipConfirmed] = event =>
    stringToUUID(event.partyRelationshipId).map(id =>
      PartyRelationshipConfirmed(
        partyRelationshipId = id,
        filePath = event.filePath,
        fileName = event.fileName,
        contentType = event.contentType
      )
    )

  implicit def partyRelationshipAddedV1PersistEventSerializer
    : PersistEventSerializer[PartyRelationshipAdded, PartyRelationshipAddedV1] =
    event => getPartyRelationshipV1(event.partyRelationship).map(PartyRelationshipAddedV1.of)

  implicit def partyRelationshipRejectedV1PersistEventDeserializer
    : PersistEventDeserializer[PartyRelationshipRejectedV1, PartyRelationshipRejected] = event =>
    stringToUUID(event.partyRelationshipId).map(PartyRelationshipRejected)

  implicit def partyRelationshipRejectedV1PersistEventSerializer
    : PersistEventSerializer[PartyRelationshipRejected, PartyRelationshipRejectedV1] =
    event =>
      Right[Throwable, PartyRelationshipRejectedV1](PartyRelationshipRejectedV1.of(event.partyRelationshipId.toString))

  implicit def partyRelationshipDeletedV1PersistEventDeserializer
    : PersistEventDeserializer[PartyRelationshipDeletedV1, PartyRelationshipDeleted] = event =>
    stringToUUID(event.partyRelationshipId).map(PartyRelationshipDeleted)

  implicit def partyRelationshipDeletedV1PersistEventSerializer
    : PersistEventSerializer[PartyRelationshipDeleted, PartyRelationshipDeletedV1] =
    event =>
      Right[Throwable, PartyRelationshipDeletedV1](PartyRelationshipDeletedV1.of(event.partyRelationshipId.toString))

  implicit def partyRelationshipSuspendedV1PersistEventDeserializer
    : PersistEventDeserializer[PartyRelationshipSuspendedV1, PartyRelationshipSuspended] = event =>
    stringToUUID(event.partyRelationshipId).map(PartyRelationshipSuspended)

  implicit def partyRelationshipSuspendedV1PersistEventSerializer
    : PersistEventSerializer[PartyRelationshipSuspended, PartyRelationshipSuspendedV1] =
    event =>
      Right[Throwable, PartyRelationshipSuspendedV1](
        PartyRelationshipSuspendedV1.of(event.partyRelationshipId.toString)
      )

  implicit def partyRelationshipActivatedV1PersistEventDeserializer
    : PersistEventDeserializer[PartyRelationshipActivatedV1, PartyRelationshipActivated] = event =>
    stringToUUID(event.partyRelationshipId).map(PartyRelationshipActivated)

  implicit def partyRelationshipActivatedV1PersistEventSerializer
    : PersistEventSerializer[PartyRelationshipActivated, PartyRelationshipActivatedV1] =
    event =>
      Right[Throwable, PartyRelationshipActivatedV1](
        PartyRelationshipActivatedV1.of(event.partyRelationshipId.toString)
      )

  implicit def tokenAddedV1PersistEventDeserializer: PersistEventDeserializer[TokenAddedV1, TokenAdded] = event =>
    getToken(event.token).map(TokenAdded)

  implicit def tokenAddedV1PersistEventSerializer: PersistEventSerializer[TokenAdded, TokenAddedV1] =
    event => getTokenV1(event.token).map(TokenAddedV1.of)

  implicit def tokenDeletedV1PersistEventDeserializer: PersistEventDeserializer[TokenDeletedV1, TokenDeleted] = event =>
    getToken(event.token).map(TokenDeleted)

  implicit def tokenDeletedV1PersistEventSerializer: PersistEventSerializer[TokenDeleted, TokenDeletedV1] =
    event => getTokenV1(event.token).map(TokenDeletedV1.of)

}
