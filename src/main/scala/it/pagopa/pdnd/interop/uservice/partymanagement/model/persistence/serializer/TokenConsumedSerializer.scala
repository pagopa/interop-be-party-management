package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.TokenConsumed
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1._

import java.io.NotSerializableException

class TokenConsumedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"
  final val version2: String = "2"

  final val currentVersion: String = version2

  override def identifier: Int = 30002

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val TokenConsumedManifest: String = classOf[TokenConsumed].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event: TokenConsumed => serialize(event, TokenConsumedManifest, currentVersion)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest.split('|').toList match {
    case TokenConsumedManifest :: `version1` :: Nil =>
      deserialize(v1.events.TokenConsumedV1, bytes, manifest, currentVersion)
    case TokenConsumedManifest :: `version2` :: Nil =>
      deserialize(v2.events.TokenConsumedV2, bytes, manifest, currentVersion)
    case _ =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
      )

  }

}
