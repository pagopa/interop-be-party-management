package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import java.util.UUID

final case class PartyRelationShipId(from: UUID, to: UUID, role: PartyRole) {
  def stringify: String = s"${from.toString}-${to.toString}-${role.stringify}"
}
