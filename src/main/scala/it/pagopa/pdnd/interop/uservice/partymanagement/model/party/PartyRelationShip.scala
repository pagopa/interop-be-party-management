package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import java.time.OffsetDateTime
import java.util.UUID

final case class PartyRelationShip(
  from: UUID,
  to: UUID,
  role: PartRole,
  start: OffsetDateTime,
  end: Option[OffsetDateTime]
)

object PartyRelationShip {

  //TODO add role check
  def create(from: Party, to: Party, role: PartRole, end: Option[OffsetDateTime]): PartyRelationShip =
    PartyRelationShip(from = from.id, to = to.id, role = role, start = OffsetDateTime.now(), end = end)
}
