package it.pagopa.pdnd.interop.uservice.partymanagement.service

import java.util.UUID

trait UUIDSupplier {
  def get: UUID
}
