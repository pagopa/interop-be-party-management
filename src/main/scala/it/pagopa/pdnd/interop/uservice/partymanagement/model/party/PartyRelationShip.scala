package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import java.time.Instant

final case class PartyRelationShip(
  from: String,
  to: String,
  relationShipType: RelationShipType,
  start: Instant,
  end: Option[Instant]
)
