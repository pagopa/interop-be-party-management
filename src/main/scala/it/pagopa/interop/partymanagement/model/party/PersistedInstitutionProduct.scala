package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.partymanagement.model.InstitutionProduct

final case class PersistedInstitutionProduct(
  product: String,
  pricingPlan: Option[String] = None,
  billing: PersistedBilling
)

object PersistedInstitutionProduct {
  def toApi(p: PersistedInstitutionProduct): InstitutionProduct =
    InstitutionProduct(product = p.product, pricingPlan = p.pricingPlan, billing = p.billing.toBilling)

  def fromApi(p: InstitutionProduct): PersistedInstitutionProduct =
    PersistedInstitutionProduct(
      product = p.product,
      pricingPlan = p.pricingPlan,
      billing = PersistedBilling.fromBilling(p.billing)
    )
}
