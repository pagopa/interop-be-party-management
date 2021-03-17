package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.uuidFormat
import java.util.UUID

final case class PartyRelationShipId(from: UUID, to: UUID, role: PartyRole) {
  def stringify: String = s"${from.toString}-${to.toString}-${role.stringify}"
}

object PartyRelationShipId extends DefaultJsonProtocol {
  implicit val personFormat: RootJsonFormat[PartyRelationShipId] = jsonFormat3(PartyRelationShipId.apply)
}
