package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import java.time.OffsetDateTime
import java.util.UUID

final case class PartyRelationShip(
  id: PartyRelationShipId,
  role: PartyRole,
  start: OffsetDateTime,
  end: Option[OffsetDateTime]
)

object PartyRelationShip {

  //TODO add role check
  def create(from: Party, to: Party, role: PartyRole, end: Option[OffsetDateTime]): PartyRelationShip =
    PartyRelationShip(
      PartyRelationShipId(from = from.id, to = to.id),
      role = role,
      start = OffsetDateTime.now(),
      end = end
    )
}
