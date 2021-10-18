package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.PartyRelationshipStatus.{Active, Pending}
import it.pagopa.pdnd.interop.uservice.partymanagement.service.UUIDSupplier

import java.time.OffsetDateTime
import java.util.UUID

final case class PartyRelationship(
  id: UUID,
  start: OffsetDateTime,
  end: Option[OffsetDateTime],
  from: UUID,
  to: UUID,
  role: PartyRole,
  platformRole: String,
  status: PartyRelationshipStatus,
  filePath: Option[String]
) {

  /** Returns a syntetic identifier useful to bind the party relationship with a generated token, if any.
    * @return
    */
  def applicationId: String = s"${from.toString}-${to.toString}-${role.stringify}-${platformRole}"
}

object PartyRelationship {
  //TODO add role check
  def create(
    UUIDSupplier: UUIDSupplier
  )(from: UUID, to: UUID, role: PartyRole, platformRole: String): PartyRelationship =
    PartyRelationship(
      id = UUIDSupplier.get,
      from = from,
      to = to,
      role = role,
      platformRole = platformRole,
      start = OffsetDateTime.now(),
      end = None,
      status = role match {
        case Operator => Active
        case _        => Pending
      },
      filePath = None
    )
}
