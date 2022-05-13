package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.partymanagement.model.InstitutionUpdate

final case class PersistedInstitutionUpdate(
  institutionType: Option[String],
  description: Option[String],
  digitalAddress: Option[String],
  address: Option[String],
  zipCode: Option[String],
  taxCode: Option[String]
) {
  def toInstitutionUpdate: InstitutionUpdate = InstitutionUpdate(
    institutionType = institutionType,
    description = description,
    digitalAddress = digitalAddress,
    address = address,
    zipCode = zipCode,
    taxCode = taxCode
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
      taxCode = institutionUpdate.taxCode
    )
}
