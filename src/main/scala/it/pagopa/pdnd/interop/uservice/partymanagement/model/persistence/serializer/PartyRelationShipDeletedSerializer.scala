package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyRelationShipDeleted
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1._

import java.io.NotSerializableException

class PartyRelationShipDeletedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"
  final val version2: String = "2"

  final val currentVersion: String = version2

  override def identifier: Int = 20002

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val PartyRelationShipDeletedManifest: String = classOf[PartyRelationShipDeleted].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event: PartyRelationShipDeleted => serialize(event, PartyRelationShipDeletedManifest, currentVersion)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest.split('|').toList match {
    case PartyRelationShipDeletedManifest :: `version1` :: Nil =>
      deserialize(v1.events.PartyRelationShipDeletedV1, bytes, manifest, version1)
    case PartyRelationShipDeletedManifest :: `version2` :: Nil =>
      deserialize(v2.events.PartyRelationShipDeletedV2, bytes, manifest, version2)
    case _ =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
      )

  }

}
