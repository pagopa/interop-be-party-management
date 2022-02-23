package it.pagopa.interop.partymanagement.service

import java.time.OffsetDateTime

trait OffsetDateTimeSupplier {
  def get: OffsetDateTime
}
