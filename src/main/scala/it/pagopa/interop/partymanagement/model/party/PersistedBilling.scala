package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.partymanagement.model.Billing

final case class PersistedBilling(vatNumber: String, recipientCode: String, publicServices: Option[Boolean]) {
  def toBilling: Billing =
    Billing(vatNumber = vatNumber, recipientCode = recipientCode, publicServices = publicServices)
}

object PersistedBilling {
  def fromBilling(billing: Billing): PersistedBilling =
    PersistedBilling(
      vatNumber = billing.vatNumber,
      recipientCode = billing.recipientCode,
      publicServices = billing.publicServices
    )
}
