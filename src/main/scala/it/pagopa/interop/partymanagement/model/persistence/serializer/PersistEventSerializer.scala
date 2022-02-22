package it.pagopa.interop.partymanagement.model.persistence.serializer

import it.pagopa.interop.partymanagement.common.utils.ErrorOr

trait PersistEventSerializer[A, B] {
  def to(a: A): ErrorOr[B]
}

object PersistEventSerializer {
  def to[A, B](a: A)(implicit e: PersistEventSerializer[A, B]): ErrorOr[B] = e.to(a)
}
