package it.pagopa.interop.partymanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.interop.partymanagement.model.persistence.PartyRelationshipSuspended
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1._

import java.io.NotSerializableException

class PartyRelationshipSuspendedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 200003

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val className: String = classOf[PartyRelationshipSuspended].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event: PartyRelationshipSuspended => serialize(event, className, currentVersion)
    case _                                 =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[${manifest(o)}]], currentVersion: [[$currentVersion]] "
      )
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest.split('|').toList match {
    case `className` :: `version1` :: Nil =>
      deserialize(v1.events.PartyRelationshipSuspendedV1, bytes, manifest, version1)
    case _                                =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
      )

  }

}
