package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.ErrorOr

trait PersistEventSerializer[A, B] {
  def to(a: A): ErrorOr[B]
}

object PersistEventSerializer {
  def to[A, B](a: A)(implicit e: PersistEventSerializer[A, B]): ErrorOr[B] = e.to(a)
}
