package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import java.time.OffsetDateTime
import java.util.UUID

final case class PartyRelationShip(
  id: PartyRelationShipId,
  start: OffsetDateTime,
  end: Option[OffsetDateTime],
  status: PartyRelationShipStatus
)

object PartyRelationShip {

  //TODO add role check
  def create(from: UUID, to: UUID, role: PartyRole): PartyRelationShip =
    PartyRelationShip(
      PartyRelationShipId(from = from, to = to, role = role),
      start = OffsetDateTime.now(),
      end = None,
      status = Pending
    )
}
