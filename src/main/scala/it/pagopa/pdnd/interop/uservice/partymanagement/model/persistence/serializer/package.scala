package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

package object serializer {
  def fromBytes[A <: GeneratedMessage](msg: GeneratedMessageCompanion[A], bytes: Array[Byte])(
    handler: A => AnyRef
  ): AnyRef = handler(msg.parseFrom(bytes))
}
