package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.partymanagement.model.DataProtectionOfficer

final case class PersistedDataProtectionOfficer(
  address: Option[String] = None,
  email: Option[String] = None,
  pec: Option[String] = None
)

object PersistedDataProtectionOfficer {
  def toApi(dataProtectionOfficer: PersistedDataProtectionOfficer): DataProtectionOfficer =
    DataProtectionOfficer(
      address = dataProtectionOfficer.address,
      email = dataProtectionOfficer.email,
      pec = dataProtectionOfficer.pec
    )

  def fromApi(dataProtectionOfficer: DataProtectionOfficer): PersistedDataProtectionOfficer =
    PersistedDataProtectionOfficer(
      address = dataProtectionOfficer.address,
      email = dataProtectionOfficer.email,
      pec = dataProtectionOfficer.pec
    )
}
