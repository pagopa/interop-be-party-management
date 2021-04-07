package it.pagopa.pdnd.interop.uservice.partymanagement.service.impl

import it.pagopa.pdnd.interop.uservice.partymanagement.service.UUIDSupplier

import java.util.UUID

class UUIDSupplierImpl extends UUIDSupplier {
  override def get: UUID = UUID.randomUUID()
}
