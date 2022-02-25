package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyRelationshipDeleted
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1._

import java.io.NotSerializableException

class PartyRelationshipDeletedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 200001

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val PartyRelationshipDeletedManifest: String = classOf[PartyRelationshipDeleted].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event: PartyRelationshipDeleted => serialize(event, PartyRelationshipDeletedManifest, currentVersion)
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest.split('|').toList match {
    case PartyRelationshipDeletedManifest :: `version1` :: Nil =>
      deserialize(v1.events.PartyRelationshipDeletedV1, bytes, manifest, version1)
    case _ =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
      )

  }

}
