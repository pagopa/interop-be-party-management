package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import java.time.OffsetDateTime
import java.util.UUID

final case class RelationShip(
  id: RelationShipId,
  start: OffsetDateTime,
  end: Option[OffsetDateTime],
  status: RelationShipStatus
)

object RelationShip {

  //TODO add role check
  def create(from: UUID, to: UUID, role: PartyRole): RelationShip =
    RelationShip(
      RelationShipId(from = from, to = to, role = role),
      start = OffsetDateTime.now(),
      end = None,
      status = RelationShipStatus.Pending
    )
}
