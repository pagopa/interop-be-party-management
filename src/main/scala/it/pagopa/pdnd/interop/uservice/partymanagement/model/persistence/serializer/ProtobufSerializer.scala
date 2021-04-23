package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

trait ProtobufSerializer[A, B] {
  def to(a: A): Either[Throwable, B]
}

object ProtobufSerializer {
  def to[A, B](a: A)(implicit e: ProtobufSerializer[A, B]): Either[Throwable, B] = e.to(a)
}
