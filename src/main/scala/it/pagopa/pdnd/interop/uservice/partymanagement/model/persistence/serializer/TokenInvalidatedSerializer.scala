package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.TokenInvalidated
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1._

import java.io.NotSerializableException

class TokenInvalidatedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 30003

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val TokenInvalidatedManifest: String = classOf[TokenInvalidated].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case TokenInvalidated(token) => v1.events.TokenInvalidatedV1(token.toTokenV1).toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {

    manifest.split('|').toList match {
      case TokenInvalidatedManifest :: `version1` :: Nil =>
        fromBytes(v1.events.TokenInvalidatedV1, bytes) { msg => TokenInvalidated(msg.token.toToken) }

      case _ =>
        throw new NotSerializableException(
          s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
        )

    }

  }

}
