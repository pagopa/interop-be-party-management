package it.pagopa.interop.partymanagement.service.impl

import it.pagopa.interop.partymanagement.service.OffsetDateTimeSupplier

import java.time.{OffsetDateTime, ZoneOffset}

case object OffsetDateTimeSupplierImp extends OffsetDateTimeSupplier {
  override def get: OffsetDateTime = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
}
