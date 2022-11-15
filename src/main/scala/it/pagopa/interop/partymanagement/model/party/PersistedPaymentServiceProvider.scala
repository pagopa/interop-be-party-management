package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.partymanagement.model.PaymentServiceProvider

final case class PersistedPaymentServiceProvider(
  abiCode: Option[String],
  businessRegisterNumber: Option[String],
  legalRegisterName: Option[String],
  legalRegisterNumber: Option[String],
  vatNumberGroup: Option[Boolean]
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
