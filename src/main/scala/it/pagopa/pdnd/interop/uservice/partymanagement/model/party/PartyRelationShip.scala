package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import java.time.OffsetDateTime
import java.util.UUID

final case class PartyRelationship(
  id: PartyRelationshipId,
  start: OffsetDateTime,
  end: Option[OffsetDateTime],
  status: PartyRelationshipStatus
)

object PartyRelationship {

  //TODO add role check
  def create(from: UUID, to: UUID, role: PartyRole): PartyRelationship =
    PartyRelationship(
      PartyRelationshipId(from = from, to = to, role = role),
      start = OffsetDateTime.now(),
      end = None,
      status = if (role == Operator) PartyRelationshipStatus.Active else PartyRelationshipStatus.Pending
    )
}
