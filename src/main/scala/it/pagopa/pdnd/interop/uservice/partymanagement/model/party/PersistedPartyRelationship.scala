package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.model.Relationship
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.PersistedPartyRelationshipState.{Active, Pending}
import it.pagopa.pdnd.interop.uservice.partymanagement.service.UUIDSupplier

import java.time.OffsetDateTime
import java.util.UUID

final case class PersistedPartyRelationship(
  id: UUID,
  start: OffsetDateTime,
  end: Option[OffsetDateTime],
  from: UUID,
  to: UUID,
  role: PersistedPartyRole,
  products: Set[String],
  productRole: String,
  state: PersistedPartyRelationshipState,
  filePath: Option[String],
  fileName: Option[String],
  contentType: Option[String]
) {

  /** Returns a syntetic identifier useful to bind the party relationship with a generated token, if any.
    * @return
    */
  def applicationId: String = s"${from.toString}-${to.toString}-${role.toString}-$productRole"

  def toRelationship: Relationship = Relationship(
    id = id,
    from = from,
    to = to,
    role = role.toApi,
    productRole = productRole,
    products = products,
    state = state.toApi,
    filePath = filePath,
    fileName = fileName,
    contentType = contentType
  )

}

object PersistedPartyRelationship {
  //TODO add role check
  def create(uuidSupplier: UUIDSupplier)(
    from: UUID,
    to: UUID,
    role: PersistedPartyRole,
    products: Set[String],
    productRole: String
  ): PersistedPartyRelationship =
    PersistedPartyRelationship(
      id = uuidSupplier.get,
      from = from,
      to = to,
      role = role,
      products = products,
      productRole = productRole,
      start = OffsetDateTime.now(),
      end = None,
      state = role match {
        case Operator => Active
        case _        => Pending
      },
      filePath = None,
      fileName = None,
      contentType = None
    )
}
