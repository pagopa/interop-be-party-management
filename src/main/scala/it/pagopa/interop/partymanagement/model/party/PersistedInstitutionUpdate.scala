package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.partymanagement.model.InstitutionUpdate
import it.pagopa.interop.partymanagement.model.party.PersistedDataProtectionOfficer.toApi
import it.pagopa.interop.partymanagement.model.party.PersistedPaymentServiceProvider.toAPi

final case class PersistedInstitutionUpdate(
  institutionType: Option[String],
  description: Option[String],
  digitalAddress: Option[String],
  address: Option[String],
  zipCode: Option[String],
  taxCode: Option[String],
  paymentServiceProvider: Option[PersistedPaymentServiceProvider],
  dataProtectionOfficer: Option[PersistedDataProtectionOfficer],
  geographicTaxonomies: Seq[PersistedGeographicTaxonomy],
  rea: Option[String],
  shareCapital: Option[String],
  businessRegisterPlace: Option[String],
  supportEmail: Option[String],
  supportPhone: Option[String],
  imported: Option[Boolean]
) {
  def toInstitutionUpdate: InstitutionUpdate = InstitutionUpdate(
    institutionType = institutionType,
    description = description,
    digitalAddress = digitalAddress,
    address = address,
    zipCode = zipCode,
    taxCode = taxCode,
    paymentServiceProvider = paymentServiceProvider.map(toAPi),
    dataProtectionOfficer = dataProtectionOfficer.map(toApi),
    geographicTaxonomies = geographicTaxonomies.map(PersistedGeographicTaxonomy.toApi),
    rea = rea,
    shareCapital = shareCapital,
    businessRegisterPlace = businessRegisterPlace,
    supportEmail = supportEmail,
    supportPhone = supportPhone,
    imported = imported
  )
}

object PersistedInstitutionUpdate {
  def fromInstitutionUpdate(institutionUpdate: InstitutionUpdate): PersistedInstitutionUpdate =
    PersistedInstitutionUpdate(
      institutionType = institutionUpdate.institutionType,
      description = institutionUpdate.description,
      digitalAddress = institutionUpdate.digitalAddress,
      address = institutionUpdate.address,
      zipCode = institutionUpdate.zipCode,
      taxCode = institutionUpdate.taxCode,
      paymentServiceProvider = institutionUpdate.paymentServiceProvider.map(PersistedPaymentServiceProvider.fromApi),
      dataProtectionOfficer = institutionUpdate.dataProtectionOfficer.map(PersistedDataProtectionOfficer.fromApi),
      geographicTaxonomies = institutionUpdate.geographicTaxonomies.map(PersistedGeographicTaxonomy.fromApi),
      rea = institutionUpdate.rea,
      shareCapital = institutionUpdate.shareCapital,
      businessRegisterPlace = institutionUpdate.businessRegisterPlace,
      supportEmail = institutionUpdate.supportEmail,
      supportPhone = institutionUpdate.supportPhone,
      imported = institutionUpdate.imported
    )
}
