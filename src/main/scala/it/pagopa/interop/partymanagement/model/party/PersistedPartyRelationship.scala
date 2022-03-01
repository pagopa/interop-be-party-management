package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.commons.utils.service.UUIDSupplier
import it.pagopa.interop.partymanagement.model.party.PersistedPartyRelationshipState.{Active, Pending}
import it.pagopa.interop.partymanagement.model.{Relationship, RelationshipProductSeed}
import it.pagopa.interop.partymanagement.service.OffsetDateTimeSupplier

import java.time.OffsetDateTime
import java.util.UUID

final case class PersistedPartyRelationship(
  id: UUID,
  from: UUID,
  to: UUID,
  role: PersistedPartyRole,
  product: PersistedProduct,
  state: PersistedPartyRelationshipState,
  filePath: Option[String],
  fileName: Option[String],
  contentType: Option[String],
  onboardingTokenId: Option[UUID],
  createdAt: OffsetDateTime,
  updatedAt: Option[OffsetDateTime]
) {

  def toRelationship: Relationship = Relationship(
    id = id,
    from = from,
    to = to,
    role = role.toApi,
    product = product.toRelationshipProduct,
    state = state.toApi,
    filePath = filePath,
    fileName = fileName,
    contentType = contentType,
    tokenId = onboardingTokenId,
    createdAt = createdAt,
    updatedAt = updatedAt
  )

}

object PersistedPartyRelationship {
  //TODO add role check
  def create(
    uuidSupplier: UUIDSupplier,
    offsetDateTimeSupplier: OffsetDateTimeSupplier
  )(from: UUID, to: UUID, role: PersistedPartyRole, product: RelationshipProductSeed): PersistedPartyRelationship = {
    val timestamp = offsetDateTimeSupplier.get
    PersistedPartyRelationship(
      id = uuidSupplier.get,
      from = from,
      to = to,
      role = role,
      product = PersistedProduct.fromRelationshipProduct(product, timestamp),
      createdAt = timestamp,
      updatedAt = None,
      state = role match {
        case PersistedPartyRole.SubDelegate | PersistedPartyRole.Operator => Active
        case PersistedPartyRole.Manager | PersistedPartyRole.Delegate     => Pending
      },
      filePath = None,
      fileName = None,
      contentType = None,
      onboardingTokenId = None
    )
  }
}