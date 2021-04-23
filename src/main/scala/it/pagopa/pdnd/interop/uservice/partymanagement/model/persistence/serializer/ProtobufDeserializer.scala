package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

trait ProtobufDeserializer[A, B] {
  def from(a: A): Either[Throwable, B]
}

object ProtobufDeserializer {
  def from[A, B](a: A)(implicit e: ProtobufDeserializer[A, B]): Either[Throwable, B] = e.from(a)
}
