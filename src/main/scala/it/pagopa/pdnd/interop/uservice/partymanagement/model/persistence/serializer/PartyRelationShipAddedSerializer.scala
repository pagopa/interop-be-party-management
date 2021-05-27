package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyRelationShipAdded
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1._

import java.io.NotSerializableException

class PartyRelationShipAddedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"
  final val version2: String = "2"

  final val currentVersion: String = version2

  override def identifier: Int = 20001

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val PartyRelationShipAddedManifest: String = classOf[PartyRelationShipAdded].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event: PartyRelationShipAdded => serialize(event, PartyRelationShipAddedManifest, currentVersion)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest.split('|').toList match {
    case PartyRelationShipAddedManifest :: `version1` :: Nil =>
      deserialize(v1.events.PartyRelationShipAddedV1, bytes, manifest, version1)
    case PartyRelationShipAddedManifest :: `version2` :: Nil =>
      deserialize(v2.events.PartyRelationShipAddedV2, bytes, manifest, version2)
    case _ =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
      )

  }

}
