package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._

import java.util.UUID

final case class State(
  parties: Map[UUID, Party],
  indexes: Map[String, UUID],
  tokens: Map[UUID, Token],
  relationShips: Map[PartyRelationShipId, PartyRelationShip]
) extends Persistable {

  def addParty(party: Party): State =
    copy(parties = parties + (party.id -> party), indexes = indexes + (party.externalId -> party.id))

  def deleteParty(party: Party): State = copy(parties = parties - party.id, indexes = indexes - party.externalId)

  def updateParty(party: Party): State =
    copy(parties = parties + (party.id -> party))

  def addPartyRelationShip(relationShip: PartyRelationShip): State =
    copy(relationShips = relationShips + (relationShip.id -> relationShip))

  def deletePartyRelationShip(relationShipId: PartyRelationShipId): State =
    copy(relationShips = relationShips - relationShipId)

  def addToken(token: Token): State = {
    copy(tokens = tokens + (token.seed -> token))
  }

  def invalidateToken(token: Token): State =
    changeTokenStatus(token, Invalid)

  def consumeToken(token: Token): State =
    changeTokenStatus(token, Consumed)

  private def changeTokenStatus(token: Token, status: TokenStatus): State = {
    val modified = tokens.get(token.seed).map(t => t.copy(status = status))

    modified match {
      case Some(t) if status == Consumed =>
        val updated: Seq[PartyRelationShip] =
          token.legals.map(legal => relationShips(legal).copy(status = PartyRelationShipStatus.Active))
        copy(relationShips = relationShips ++ updated.map(p => p.id -> p).toMap, tokens = tokens + (t.seed -> t))
      case Some(t) => copy(tokens = tokens + (t.seed -> t))
      case None    => this
    }

  }

}

object State {
  val empty: State = State(parties = Map.empty, indexes = Map.empty, relationShips = Map.empty, tokens = Map.empty)
}
