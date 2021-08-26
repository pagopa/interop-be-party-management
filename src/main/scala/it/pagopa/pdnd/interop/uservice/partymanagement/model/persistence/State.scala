package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import org.slf4j.LoggerFactory

import java.util.UUID

@SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Equals"))
final case class State(
  parties: Map[UUID, Party],  //TODO use String instead of UUID
  indexes: Map[String, UUID], //TODO use String instead of UUID
  tokens: Map[String, Token],
  relationships: Map[PartyRelationshipId, PartyRelationship]
) extends Persistable {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def addParty(party: Party): State = {
    logger.error(s"Writing party ${party.externalId} to state")
    val newState = copy(parties = parties + (party.id -> party), indexes = indexes + (party.externalId -> party.id))
    logger.info(newState.toString)
    newState
  }

  def deleteParty(party: Party): State = copy(parties = parties - party.id, indexes = indexes - party.externalId)

  def updateParty(party: Party): State =
    copy(parties = parties + (party.id -> party))

  def addPartyRelationship(relationship: PartyRelationship): State =
    copy(relationships = relationships + (relationship.id -> relationship))

  def confirmPartyRelationship(relationshipId: PartyRelationshipId): State = {
    val updated: Map[PartyRelationshipId, PartyRelationship] =
      relationships.updated(relationshipId, relationships(relationshipId).copy(status = PartyRelationshipStatus.Active))
    copy(relationships = updated)
  }

  def deletePartyRelationship(relationshipId: PartyRelationshipId): State =
    copy(relationships = relationships - relationshipId)

  def addToken(token: Token): State = copy(tokens = tokens + (token.id -> token))

  def deleteToken(token: Token): State = copy(tokens = tokens - token.id)

}

object State {
  val empty: State =
    State(
      parties = Map.empty[UUID, Party],
      indexes = Map.empty[String, UUID],
      relationships = Map.empty[PartyRelationshipId, PartyRelationship],
      tokens = Map.empty[String, Token]
    )
}
