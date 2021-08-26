package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.uuidFormat
import java.util.UUID

final case class PartyRelationshipId(from: UUID, to: UUID, role: PartyRole) {
  def stringify: String = s"${from.toString}-${to.toString}-${role.stringify}"
}

object PartyRelationshipId extends DefaultJsonProtocol {
  implicit val personFormat: RootJsonFormat[PartyRelationshipId] = jsonFormat3(PartyRelationshipId.apply)
}
