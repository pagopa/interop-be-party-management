package it.pagopa.pdnd.interop.uservice.partymanagement.service.impl

import it.pagopa.pdnd.interop.uservice.partymanagement.service.OffsetDateTimeSupplier

import java.time.OffsetDateTime

case object OffsetDateTimeSupplierImp extends OffsetDateTimeSupplier {
  override def get: OffsetDateTime = OffsetDateTime.now()
}
