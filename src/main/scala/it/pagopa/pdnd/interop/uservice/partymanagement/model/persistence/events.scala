package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{
  Party,
  PartyRelationship,
  PartyRelationshipId,
  Token
}

/* Event */
sealed trait Event                  extends Persistable
sealed trait PartyEvent             extends Event
sealed trait PartyRelationshipEvent extends Event
sealed trait TokenEvent             extends Event

/* Party Event */
final case class PartyAdded(party: Party)      extends PartyEvent
final case class PartyDeleted(party: Party)    extends PartyEvent
final case class AttributesAdded(party: Party) extends PartyEvent

/* PartyRelationship Event */
final case class PartyRelationshipAdded(partyRelationship: PartyRelationship)         extends PartyRelationshipEvent
final case class PartyRelationshipConfirmed(partyRelationshipId: PartyRelationshipId) extends PartyRelationshipEvent
final case class PartyRelationshipDeleted(partyRelationshipId: PartyRelationshipId)   extends PartyRelationshipEvent

/* Token Event */
final case class TokenAdded(token: Token)   extends TokenEvent
final case class TokenDeleted(token: Token) extends TokenEvent
//final case class TokenInvalidated(token: Token) extends TokenEvent
//final case class TokenConsumed(token: Token)    extends TokenEvent
