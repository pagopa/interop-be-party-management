package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import java.time.OffsetDateTime

final case class PartyRelationShip(
  id: PartyRelationShipId,
  start: OffsetDateTime,
  end: Option[OffsetDateTime],
  status: PartyRelationShipStatus
)

object PartyRelationShip {

  //TODO add role check
  def create(from: Party, to: Party, role: PartyRole): PartyRelationShip =
    PartyRelationShip(
      PartyRelationShipId(from = from.id, to = to.id, role = role),
      start = OffsetDateTime.now(),
      end = None,
      status = Pending
    )
}
