package it.pagopa.pdnd.interop.uservice.partymanagement.service

import java.time.OffsetDateTime

trait OffsetDateTimeSupplier {
  def get: OffsetDateTime
}
