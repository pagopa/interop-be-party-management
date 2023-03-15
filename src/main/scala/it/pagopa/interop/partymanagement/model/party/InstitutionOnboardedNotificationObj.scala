package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.partymanagement.model.{PaymentServiceProvider, Relationship, RelationshipState}

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.UUID

final case class InstitutionOnboardedNotification(
  id: Option[UUID],
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
  updatedAt: Option[OffsetDateTime],
  closedAt: Option[OffsetDateTime],
  createdAt: OffsetDateTime,
  psp: Option[PSP]
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

case class PSP(
  abiCode: Option[String],
  vatNumberGroup: Option[Boolean],
  contractId: Option[String],
  providerNames: Option[String],
  contractType: Option[String],
  courtesyMail: Option[String],
  referenteFatturaMail: Option[String],
  sdd: Option[String],
  membershipId: Option[String],
  recipientId: Option[String]
)

case class InstitutionOnboardedBilling(vatNumber: String, recipientCode: String, publicServices: Option[Boolean])

object InstitutionOnboardedNotificationObj {
  def toNotification(
    institution: InstitutionParty,
    productId: String,
    relationship: Relationship
  ): InstitutionOnboardedNotification = {
    val productInfo = institution.products
      .find(p => productId == p.product)

    val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

    InstitutionOnboardedNotification(
      id = relationship.tokenId,
      internalIstitutionID = institution.id,
      product = productId,
      state = relationship.state match {
        case RelationshipState.DELETED => "CLOSED"
        case _                         => "ACTIVE"
      },
      filePath = relationship.filePath,
      fileName = relationship.fileName,
      contentType = relationship.contentType,
      onboardingTokenId = relationship.tokenId,
      pricingPlan = productInfo.flatMap(_.pricingPlan),
      institution = InstitutionOnboardedObj.fromInstitution(institution),
      billing = productInfo.map(p => InstitutionOnboardedBillingObj.fromInstitutionProduct(p.billing)),
      updatedAt = relationship.updatedAt.map(t =>
        LocalDateTime.parse(OffsetDateTime.from(t).format(FORMATTER)).atZone(ZoneOffset.UTC).toOffsetDateTime
      ),
      createdAt = LocalDateTime
        .parse(OffsetDateTime.from(relationship.createdAt).format(FORMATTER))
        .atZone(ZoneOffset.UTC)
        .toOffsetDateTime,
      closedAt = relationship.state match {
        case RelationshipState.DELETED =>
          relationship.updatedAt.map(t =>
            LocalDateTime.parse(OffsetDateTime.from(t).format(FORMATTER)).atZone(ZoneOffset.UTC).toOffsetDateTime
          )
        case _                         => Option.empty
      },
      psp = institution.institutionType.getOrElse("") match {
        case "PSP" =>
          relationship.institutionUpdate
            .flatMap(_.paymentServiceProvider)
            .map(p => PSPObj.fromPaymentServiceProvider(p))
        case _     => Option.empty
      }
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

object PSPObj {
  def fromPaymentServiceProvider(paymentServiceProvider: PaymentServiceProvider): PSP = PSP(
    abiCode = paymentServiceProvider.abiCode,
    vatNumberGroup = paymentServiceProvider.vatNumberGroup,
    contractId = Option.empty,
    providerNames = Option.empty,
    contractType = Option.empty,
    courtesyMail = Option.empty,
    referenteFatturaMail = Option.empty,
    sdd = Option.empty,
    membershipId = Option.empty,
    recipientId = Option.empty
  )
}
