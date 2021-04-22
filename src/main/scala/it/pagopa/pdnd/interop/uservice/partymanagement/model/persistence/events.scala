package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{
  Party,
  PartyRelationShip,
  PartyRelationShipId,
  Token
}

/* Event */
sealed trait Event
sealed trait PartyEvent             extends Event
sealed trait PartyRelationShipEvent extends Event
sealed trait TokenEvent             extends Event

/* Party Event */
final case class PartyAdded(party: Party)   extends PartyEvent
final case class PartyDeleted(party: Party) extends PartyEvent

/* PartyRelationShip Event */
final case class PartyRelationShipAdded(partyRelationShip: PartyRelationShip)  extends PartyRelationShipEvent
final case class PartyRelationShipDeleted(relationShipId: PartyRelationShipId) extends PartyRelationShipEvent

/* Token Event */
final case class TokenAdded(token: Token)       extends TokenEvent
final case class TokenInvalidated(token: Token) extends TokenEvent
final case class TokenConsumed(token: Token)    extends TokenEvent
