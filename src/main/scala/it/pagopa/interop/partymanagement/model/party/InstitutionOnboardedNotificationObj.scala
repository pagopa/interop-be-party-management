package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.partymanagement.model.persistence.PartyRelationshipConfirmed

import java.time.OffsetDateTime
import java.util.UUID

final case class InstitutionOnboardedNotification(
  internalIstitutionID: UUID,
  product: String,
  state: String,
  filePath: Option[String],
  fileName: Option[String],
  contentType: Option[String],
  onboardingTokenId: Option[UUID],
  pricingPlan: Option[String],
  institution: InstitutionOnboarded,
  billing: Option[InstitutionOnboardedBilling],
  updatedAt: Option[OffsetDateTime]
)

case class InstitutionOnboarded(
  institutionType: String,
  description: String,
  digitalAddress: Option[String],
  address: Option[String],
  taxCode: String,
  origin: String,
  originId: String
)

case class InstitutionOnboardedBilling(vatNumber: String, recipientCode: String, publicServices: Option[Boolean])

object InstitutionOnboardedNotificationObj {
  def toNotification(
    institution: InstitutionParty,
    productId: String,
    managerRelationshipConfirm: PartyRelationshipConfirmed
  ): InstitutionOnboardedNotification = {
    val productInfo = institution.products
      .find(p => productId == p.product)

    InstitutionOnboardedNotification(
      internalIstitutionID = institution.id,
      product = productId,
      state = "ACTIVE",
      filePath = Option(managerRelationshipConfirm.filePath),
      fileName = Option(managerRelationshipConfirm.fileName),
      contentType = Option(managerRelationshipConfirm.contentType),
      onboardingTokenId = Option(managerRelationshipConfirm.onboardingTokenId),
      pricingPlan = productInfo.flatMap(_.pricingPlan),
      institution = InstitutionOnboardedObj.fromInstitution(institution),
      billing = productInfo.map(p => InstitutionOnboardedBillingObj.fromInstitutionProduct(p.billing)),
      updatedAt = Option(managerRelationshipConfirm.timestamp)
    )
  }
}

object InstitutionOnboardedObj {
  def fromInstitution(institution: InstitutionParty): InstitutionOnboarded = InstitutionOnboarded(
    institutionType = institution.institutionType.getOrElse(""),
    description = institution.description,
    digitalAddress = Option(institution.digitalAddress),
    address = Option(institution.address),
    taxCode = institution.taxCode,
    origin = institution.origin,
    originId = institution.originId
  )
}

object InstitutionOnboardedBillingObj {
  def fromInstitutionProduct(billing: PersistedBilling): InstitutionOnboardedBilling = InstitutionOnboardedBilling(
    vatNumber = billing.vatNumber,
    recipientCode = billing.recipientCode,
    publicServices = billing.publicServices
  )
}
