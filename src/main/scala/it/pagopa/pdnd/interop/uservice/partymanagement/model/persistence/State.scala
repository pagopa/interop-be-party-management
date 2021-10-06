package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import org.slf4j.LoggerFactory

import java.util.UUID

@SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Equals"))
final case class State(
  parties: Map[UUID, Party],  //TODO use String instead of UUID
  indexes: Map[String, UUID], //TODO use String instead of UUID
  tokens: Map[String, Token],
  relationships: Map[String, PartyRelationship]
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
    copy(relationships = relationships + (relationship.id.toString -> relationship))

  def confirmPartyRelationship(id: UUID): State = {
    val relationshipId = id.toString
    val updated: Map[String, PartyRelationship] =
      relationships.updated(relationshipId, relationships(relationshipId).copy(status = PartyRelationshipStatus.Active))
    copy(relationships = updated)
  }

  def deletePartyRelationship(relationshipId: UUID): State =
    copy(relationships = relationships - relationshipId.toString)

  def suspendRelationship(relationship: PartyRelationship): State = {
    val suspendedRelationship = relationship.copy(status = PartyRelationshipStatus.Suspended)
    copy(relationships = relationships + (relationship.id.toString -> suspendedRelationship))
  }

  def getPartyRelationshipByAttributes(
    from: UUID,
    to: UUID,
    role: PartyRole,
    platformRole: String
  ): Option[PartyRelationship] = {
    relationships.values.find(relationship =>
      from.toString == relationship.from.toString
        && to.toString == relationship.to.toString
        && role == relationship.role
        && platformRole == relationship.platformRole
    )
  }

  def addToken(token: Token): State = copy(tokens = tokens + (token.id -> token))

  def deleteToken(token: Token): State = copy(tokens = tokens - token.id)
}

object State {
  val empty: State =
    State(
      parties = Map.empty[UUID, Party],
      indexes = Map.empty[String, UUID],
      relationships = Map.empty[String, PartyRelationship],
      tokens = Map.empty[String, Token]
    )
}
