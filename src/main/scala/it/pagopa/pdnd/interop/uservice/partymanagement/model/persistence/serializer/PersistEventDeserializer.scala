package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.ErrorOr

trait PersistEventDeserializer[A, B] {
  def from(a: A): ErrorOr[B]
}

object PersistEventDeserializer {
  def from[A, B](a: A)(implicit e: PersistEventDeserializer[A, B]): ErrorOr[B] = e.from(a)
}
