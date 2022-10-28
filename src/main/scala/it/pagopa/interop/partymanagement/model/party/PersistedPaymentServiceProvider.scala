package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.partymanagement.model.PaymentServiceProvider

final case class PersistedPaymentServiceProvider(
  abiCode: Option[String] = None,
  businessRegisterNumber: Option[String] = None,
  legalRegisterName: Option[String] = None,
  legalRegisterNumber: Option[String] = None,
  vatNumberGroup: Option[Boolean] = None
)

object PersistedPaymentServiceProvider {
  def toAPi(paymentServiceProvider: PersistedPaymentServiceProvider): PaymentServiceProvider =
    PaymentServiceProvider(
      abiCode = paymentServiceProvider.abiCode,
      businessRegisterNumber = paymentServiceProvider.businessRegisterNumber,
      legalRegisterName = paymentServiceProvider.legalRegisterName,
      legalRegisterNumber = paymentServiceProvider.legalRegisterNumber,
      vatNumberGroup = paymentServiceProvider.vatNumberGroup
    )

  def fromApi(paymentServiceProvider: PaymentServiceProvider): PersistedPaymentServiceProvider =
    PersistedPaymentServiceProvider(
      abiCode = paymentServiceProvider.abiCode,
      businessRegisterNumber = paymentServiceProvider.businessRegisterNumber,
      legalRegisterName = paymentServiceProvider.legalRegisterName,
      legalRegisterNumber = paymentServiceProvider.legalRegisterNumber,
      vatNumberGroup = paymentServiceProvider.vatNumberGroup
    )
}
