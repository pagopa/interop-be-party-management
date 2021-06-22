package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.TokenInvalidated
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1._

import java.io.NotSerializableException

class TokenInvalidatedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"
  final val version2: String = "2"

  final val currentVersion: String = version2

  override def identifier: Int = 300002

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val TokenInvalidatedManifest: String = classOf[TokenInvalidated].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event: TokenInvalidated => serialize(event, TokenInvalidatedManifest, currentVersion)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest.split('|').toList match {
    case TokenInvalidatedManifest :: `version1` :: Nil =>
      deserialize(v1.events.TokenInvalidatedV1, bytes, manifest, currentVersion)
    case TokenInvalidatedManifest :: `version2` :: Nil =>
      deserialize(v2.events.TokenInvalidatedV2, bytes, manifest, currentVersion)
    case _ =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
      )

  }

}
