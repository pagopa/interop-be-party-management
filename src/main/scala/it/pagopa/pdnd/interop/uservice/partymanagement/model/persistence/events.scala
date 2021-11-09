package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{Party, PersistedPartyRelationship, Token}

import java.util.UUID

/* Event */
sealed trait Event                  extends Persistable
sealed trait PartyEvent             extends Event
sealed trait PartyRelationshipEvent extends Event
sealed trait TokenEvent             extends Event

/* Party Event */
final case class PartyAdded(party: Party)                extends PartyEvent
final case class PartyDeleted(party: Party)              extends PartyEvent
final case class AttributesAdded(party: Party)           extends PartyEvent
final case class OrganizationProductsAdded(party: Party) extends PartyEvent

/* PartyRelationship Event */
final case class PartyRelationshipAdded(partyRelationship: PersistedPartyRelationship) extends PartyRelationshipEvent
final case class PartyRelationshipProductsAdded(partyRelationshipId: UUID, products: Set[String])
    extends PartyRelationshipEvent
final case class PartyRelationshipConfirmed(
  partyRelationshipId: UUID,
  filePath: String,
  fileName: String,
  contentType: String
)                                                                      extends PartyRelationshipEvent
final case class PartyRelationshipRejected(partyRelationshipId: UUID)  extends PartyRelationshipEvent
final case class PartyRelationshipDeleted(partyRelationshipId: UUID)   extends PartyRelationshipEvent
final case class PartyRelationshipSuspended(partyRelationshipId: UUID) extends PartyRelationshipEvent
final case class PartyRelationshipActivated(partyRelationshipId: UUID) extends PartyRelationshipEvent

/* Token Event */
final case class TokenAdded(token: Token)   extends TokenEvent
final case class TokenDeleted(token: Token) extends TokenEvent
//final case class TokenInvalidated(token: Token) extends TokenEvent
//final case class TokenConsumed(token: Token)    extends TokenEvent
