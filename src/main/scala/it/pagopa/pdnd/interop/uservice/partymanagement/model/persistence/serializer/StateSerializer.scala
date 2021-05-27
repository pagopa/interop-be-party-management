package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.State
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1._

import java.io.NotSerializableException

class StateSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"
  final val version2: String = "2"

  final val currentVersion: String = version2

  override def identifier: Int = 40000

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val StateManifest: String = classOf[State].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case s: State => serialize(s, StateManifest, currentVersion)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest.split('|').toList match {
      case StateManifest :: `version1` :: Nil =>
        deserialize(v1.state.StateV1, bytes, manifest, version1)
      case StateManifest :: `version2` :: Nil =>
        deserialize(v2.state.StateV2, bytes, manifest, version2)
      case _ =>
        throw new NotSerializableException(
          s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
        )

    }

}
