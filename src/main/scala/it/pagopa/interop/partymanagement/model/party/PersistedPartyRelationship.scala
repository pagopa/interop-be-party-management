package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.commons.utils.service.UUIDSupplier
import it.pagopa.interop.partymanagement.model.party.PersistedPartyRelationshipState.{Active, Pending}
import it.pagopa.interop.partymanagement.model.{
  Billing,
  InstitutionId,
  InstitutionUpdate,
  Relationship,
  RelationshipProductSeed
}
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
  pricingPlan: Option[String],
  institutionUpdate: Option[PersistedInstitutionUpdate],
  billing: Option[PersistedBilling],
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
    pricingPlan = pricingPlan,
    institutionUpdate = institutionUpdate.map(i => i.toInstitutionUpdate),
    billing = billing.map(b => b.toBilling),
    createdAt = createdAt,
    updatedAt = updatedAt
  )

  def toInstitutionId: InstitutionId = InstitutionId(
    id = id,
    to = to,
    product = product.toRelationshipProduct.id,
    digitalAddress = institutionUpdate.flatMap(_.toInstitutionUpdate.digitalAddress).getOrElse("")
  )
}

object PersistedPartyRelationship {
  // TODO add role check
  def create(uuidSupplier: UUIDSupplier, offsetDateTimeSupplier: OffsetDateTimeSupplier)(
    from: UUID,
    to: UUID,
    role: PersistedPartyRole,
    product: RelationshipProductSeed,
    pricingPlan: Option[String],
    institutionUpdate: Option[InstitutionUpdate],
    billing: Option[Billing]
  ): PersistedPartyRelationship = {
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
      onboardingTokenId = None,
      pricingPlan = pricingPlan,
      institutionUpdate = institutionUpdate.map(i => PersistedInstitutionUpdate.fromInstitutionUpdate(i)),
      billing = billing.map(b => PersistedBilling.fromBilling(b))
    )
  }
}
