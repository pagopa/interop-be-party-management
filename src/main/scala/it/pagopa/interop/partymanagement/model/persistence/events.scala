package it.pagopa.interop.partymanagement.model.persistence

import it.pagopa.interop.partymanagement.model.{Billing, CreatedAtSeed}
import it.pagopa.interop.partymanagement.model.party.{Party, PersistedPartyRelationship, Token}

import java.time.OffsetDateTime
import java.util.UUID

/* Event */
sealed trait Event                  extends Persistable
sealed trait PartyEvent             extends Event
sealed trait PartyRelationshipEvent extends Event
sealed trait TokenEvent             extends Event

/* Party Event */
final case class PartyAdded(party: Party)                  extends PartyEvent
final case class PartyUpdated(party: Party)                extends PartyEvent
final case class PartyDeleted(party: Party)                extends PartyEvent
final case class AttributesAdded(party: Party)             extends PartyEvent
final case class PaymentServiceProviderAdded(party: Party) extends PartyEvent
final case class DataProtectionOfficerAdded(party: Party)  extends PartyEvent

/* PartyRelationship Event */
final case class PartyRelationshipAdded(partyRelationship: PersistedPartyRelationship) extends PartyRelationshipEvent
final case class PartyRelationshipConfirmed(
  partyRelationshipId: UUID,
  filePath: String,
  fileName: String,
  contentType: String,
  onboardingTokenId: UUID,
  timestamp: OffsetDateTime
) extends PartyRelationshipEvent
final case class PartyRelationshipRejected(partyRelationshipId: UUID)                  extends PartyRelationshipEvent
final case class PartyRelationshipDeleted(partyRelationshipId: UUID, timestamp: OffsetDateTime)
    extends PartyRelationshipEvent
final case class PartyRelationshipSuspended(partyRelationshipId: UUID, timestamp: OffsetDateTime)
    extends PartyRelationshipEvent
final case class PartyRelationshipActivated(partyRelationshipId: UUID, timestamp: OffsetDateTime)
    extends PartyRelationshipEvent
final case class PartyRelationshipEnabled(partyRelationshipId: UUID, timestamp: OffsetDateTime)
    extends PartyRelationshipEvent
final case class PartyRelationshipUpdateBilling(partyRelationshipId: UUID, billing: Billing, timestamp: OffsetDateTime)
    extends PartyRelationshipEvent
final case class PartyRelationshipUpdateCreatedAt(
  partyRelationshipId: UUID,
  createdAtSeed: CreatedAtSeed,
  timestamp: OffsetDateTime
) extends PartyRelationshipEvent
final case class PartyRelationshipWithId(partyRelationshipId: UUID)                    extends PartyRelationshipEvent
/* Token Event */
final case class TokenAdded(token: Token)                                              extends TokenEvent
final case class TokenUpdated(token: Token)                                            extends TokenEvent
