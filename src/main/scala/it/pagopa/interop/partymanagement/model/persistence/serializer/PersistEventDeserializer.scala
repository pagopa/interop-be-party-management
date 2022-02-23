package it.pagopa.interop.partymanagement.model.persistence.serializer

import it.pagopa.interop.partymanagement.common.utils.ErrorOr

trait PersistEventDeserializer[A, B] {
  def from(a: A): ErrorOr[B]
}

object PersistEventDeserializer {
  def from[A, B](a: A)(implicit e: PersistEventDeserializer[A, B]): ErrorOr[B] = e.from(a)
}
