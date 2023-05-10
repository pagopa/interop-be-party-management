package it.pagopa.interop.partymanagement.model.persistence

import it.pagopa.interop.partymanagement.model.{Billing, CreatedAtSeed}
import it.pagopa.interop.partymanagement.model.party._
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import java.util.UUID

final case class State(
  parties: Map[UUID, Party],
  tokens: Map[UUID, Token],
  relationships: Map[UUID, PersistedPartyRelationship]
) extends Persistable {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def addParty(party: Party): State = {
    logger.debug(s"Writing party ${party.id.toString} to state")
    val newState = copy(parties = parties + (party.id -> party))
    logger.debug(newState.toString)
    newState
  }

  def deleteParty(party: Party): State = copy(parties = parties - party.id)

  def updateParty(party: Party): State =
    copy(parties = parties + (party.id -> party))

  def addPartyRelationship(relationship: PersistedPartyRelationship): State =
    copy(relationships = relationships + (relationship.id -> relationship))

  def confirmPartyRelationship(
    relationshipId: UUID,
    filePath: String,
    fileName: String,
    contentType: String,
    tokenId: UUID,
    timestamp: OffsetDateTime
  ): State = {
    val updated: Map[UUID, PersistedPartyRelationship] =
      relationships.updated(
        relationshipId,
        relationships(relationshipId).copy(
          state = PersistedPartyRelationshipState.Active,
          filePath = Some(filePath),
          fileName = Some(fileName),
          contentType = Some(contentType),
          onboardingTokenId = Some(tokenId),
          updatedAt = Some(timestamp)
        )
      )
    copy(relationships = updated)
  }

  def rejectRelationship(relationshipId: UUID): State = copy(relationships = relationships - relationshipId)

  def suspendRelationship(relationshipId: UUID, timestamp: OffsetDateTime): State =
    updateRelationshipStatus(relationshipId, PersistedPartyRelationshipState.Suspended, timestamp)

  def activateRelationship(relationshipId: UUID, timestamp: OffsetDateTime): State =
    updateRelationshipStatus(relationshipId, PersistedPartyRelationshipState.Active, timestamp)

  def enableRelationship(relationshipId: UUID, timestamp: OffsetDateTime): State =
    updateRelationshipStatus(relationshipId, PersistedPartyRelationshipState.Pending, timestamp)

  def deleteRelationship(relationshipId: UUID, timestamp: OffsetDateTime): State =
    updateRelationshipStatus(relationshipId, PersistedPartyRelationshipState.Deleted, timestamp)

  private def updateRelationshipStatus(
    relationshipId: UUID,
    newStatus: PersistedPartyRelationshipState,
    timestamp: OffsetDateTime
  ): State =
    relationships.get(relationshipId) match {
      case Some(relationship) =>
        val updatedRelationship = relationship.copy(state = newStatus, updatedAt = Some(timestamp))
        copy(relationships = relationships + (relationship.id -> updatedRelationship))
      case None               =>
        this
    }

  def getPartyRelationshipByAttributes(
    from: UUID,
    to: UUID,
    role: PersistedPartyRole,
    product: String,
    productRole: String
  ): Option[PersistedPartyRelationship] = {
    relationships.values.find(relationship =>
      from.toString == relationship.from.toString
        && to.toString == relationship.to.toString
        && role == relationship.role
        && product == relationship.product.id
        && productRole == relationship.product.role
    )
  }

  def addToken(token: Token): State = copy(tokens = tokens + (token.id -> token))

  def updateToken(token: Token): State =
    tokens.get(token.id) match {
      case Some(t) =>
        copy(tokens = tokens + (t.id -> token))
      case None    =>
        this
    }

  def updateBilling(partyRelationshipId: UUID, billing: Billing, timestamp: OffsetDateTime): State = {
    relationships.get(partyRelationshipId) match {
      case Some(relationship) =>
        val updatedRelationship = relationship.copy(
          billing = Some(
            PersistedBilling(
              vatNumber = billing.vatNumber,
              recipientCode = billing.recipientCode,
              publicServices = billing.publicServices
            )
          ),
          updatedAt = Some(timestamp)
        )
        copy(relationships = relationships + (relationship.id -> updatedRelationship))
      case None               =>
        this
    }
  }

  def updateCreatedAt(partyRelationshipId: UUID, createdAtSeed: CreatedAtSeed, timestamp: OffsetDateTime): State = {
    relationships.get(partyRelationshipId) match {
      case Some(relationship) =>
        val updatedRelationship = relationship.copy(createdAt = createdAtSeed.createdAt, updatedAt = Some(timestamp))
        copy(relationships = relationships + (relationship.id -> updatedRelationship))
      case None               =>
        this
    }
  }

  def partyRelationship(partyRelationshipId: UUID): State = {
    relationships.get(partyRelationshipId) match {
      case Some(_) =>
        this
      case None    =>
        this
    }
  }
}

object State {
  val empty: State =
    State(
      parties = Map.empty[UUID, Party],
      relationships = Map.empty[UUID, PersistedPartyRelationship],
      tokens = Map.empty[UUID, Token]
    )
}
