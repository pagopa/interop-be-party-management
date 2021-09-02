package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.uuidFormat
import java.util.UUID

final case class PartyRelationshipId(from: UUID, to: UUID, role: PartyRole, platformRole: String) {
  def stringify: String = s"${from.toString}-${to.toString}-${role.stringify}-${platformRole}"
}

object PartyRelationshipId extends DefaultJsonProtocol {
  implicit val personFormat: RootJsonFormat[PartyRelationshipId] = jsonFormat4(PartyRelationshipId.apply)
}
