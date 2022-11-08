package it.pagopa.interop.partymanagement.persistence.v1

import it.pagopa.interop.partymanagement.model.{DataProtectionOfficer, PaymentServiceProvider}
import it.pagopa.interop.partymanagement.model.party.{
  InstitutionAttribute,
  PersistedBilling,
  PersistedDataProtectionOfficer,
  PersistedInstitutionProduct,
  PersistedInstitutionUpdate,
  PersistedPaymentServiceProvider
}

import java.time.OffsetDateTime
import java.util.UUID

object StateCommonData {
  val start     = OffsetDateTime.now()
  val end       = OffsetDateTime.now()
  val createdAt = OffsetDateTime.now()
  val updatedAt = OffsetDateTime.now()
  val timestamp = OffsetDateTime.now()

  val personPartyId      = UUID.randomUUID()
  val institutionPartyId = UUID.randomUUID()

  val externalId2            = UUID.randomUUID()
  val originId2              = UUID.randomUUID()
  val description            = "description"
  val digitalAddress         = "digitalAddress"
  val address                = "address"
  val zipCode                = "zipCode"
  val taxCode                = "taxCode"
  val origin                 = "IPA"
  val institutionType        = "PA"
  val attributes             = Seq(
    InstitutionAttribute(origin = "origin", code = "a", description = "description_a"),
    InstitutionAttribute(origin = "origin", code = "b", description = "description_b")
  )
  val products               = Set(
    PersistedInstitutionProduct(
      product = "product1",
      pricingPlan = Option("pricingPlan"),
      billing = PersistedBilling(vatNumber = "VATNUMBER", recipientCode = "RECIPIENTCODE", publicServices = None)
    ),
    PersistedInstitutionProduct(
      product = "product1",
      pricingPlan = Option("pricingPlan"),
      billing =
        PersistedBilling(vatNumber = "VATNUMBER", recipientCode = "RECIPIENTCODE", publicServices = Option(true))
    )
  )
  val paymentServiceProvider = PaymentServiceProvider(
    abiCode = Some("12345"),
    businessRegisterNumber = Some("123456"),
    legalRegisterName = Some("Register Name"),
    legalRegisterNumber = Some("1234567"),
    vatNumberGroup = Some(false)
  )
  val dataProtectionOfficer  =
    DataProtectionOfficer(address = Some("via Roma 1"), email = Some("ciao@ciao.it"), pec = Some("pec@pec.it"))

  val productId        = "productId"
  val productRole      = "productRole"
  val productCreatedAt = OffsetDateTime.now()

  val relationshipId     = UUID.randomUUID()
  val noneTestId         = UUID.randomUUID()
  val managerActiveId    = UUID.randomUUID()
  val managerPendingId   = UUID.randomUUID()
  val managerRejectedId  = UUID.randomUUID()
  val managerSuspendedId = UUID.randomUUID()
  val managerDeletedId   = UUID.randomUUID()

  val delegateActiveId    = UUID.randomUUID()
  val delegatePendingId   = UUID.randomUUID()
  val delegateRejectedId  = UUID.randomUUID()
  val delegateSuspendedId = UUID.randomUUID()
  val delegateDeletedId   = UUID.randomUUID()

  val subDelegateActiveId    = UUID.randomUUID()
  val subDelegatePendingId   = UUID.randomUUID()
  val subDelegateRejectedId  = UUID.randomUUID()
  val subDelegateSuspendedId = UUID.randomUUID()
  val subDelegateDeletedId   = UUID.randomUUID()

  val operatorActiveId    = UUID.randomUUID()
  val operatorPendingId   = UUID.randomUUID()
  val operatorRejectedId  = UUID.randomUUID()
  val operatorSuspendedId = UUID.randomUUID()
  val operatorDeletedId   = UUID.randomUUID()

  val filePath          = "filePath"
  val fileName          = "fileName"
  val contentType       = "contentType"
  val onboardingTokenId = UUID.randomUUID()

  val tokenId  = UUID.randomUUID()
  val validity = OffsetDateTime.now()
  val checksum = "checksum"
  val version  = "version"
  val path     = "path"

  val pricingPlan       = Option("PRICING_PLAN")
  val billing           = Option(PersistedBilling("VATNUMBER", "RECIPIENTCODE", Option(true)))
  val institutionUpdate = Option(
    PersistedInstitutionUpdate(
      Option("PAOVERRIDE"),
      Option("DESCRIPTIONOVERRIDE"),
      Option("MAILOVERRIDE"),
      Option("ADDRESSOVERRIDE"),
      Option("ZIPCODEOVERRIDE"),
      Option("TAXCODEOVERRIDE"),
      paymentServiceProvider = Option(
        PersistedPaymentServiceProvider(
          abiCode = paymentServiceProvider.abiCode,
          businessRegisterNumber = paymentServiceProvider.businessRegisterNumber,
          legalRegisterName = paymentServiceProvider.legalRegisterName,
          legalRegisterNumber = paymentServiceProvider.legalRegisterNumber,
          vatNumberGroup = paymentServiceProvider.vatNumberGroup
        )
      ),
      dataProtectionOfficer = Option(
        PersistedDataProtectionOfficer(
          address = dataProtectionOfficer.address,
          email = dataProtectionOfficer.email,
          pec = dataProtectionOfficer.pec
        )
      )
    )
  )

}
