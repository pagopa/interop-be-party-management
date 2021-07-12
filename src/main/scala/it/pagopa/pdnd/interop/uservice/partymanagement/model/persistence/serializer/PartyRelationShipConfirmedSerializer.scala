package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.{PartyRelationShipConfirmed, PartyRelationShipDeleted}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1._

import java.io.NotSerializableException

class PartyRelationShipConfirmedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 200002

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val PartyRelationShipConfirmedManifest: String = classOf[PartyRelationShipConfirmed].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event: PartyRelationShipDeleted => serialize(event, PartyRelationShipConfirmedManifest, currentVersion)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest.split('|').toList match {
    case PartyRelationShipConfirmedManifest :: `version1` :: Nil =>
      deserialize(v1.events.PartyRelationShipConfirmedV1, bytes, manifest, version1)
    case _ =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
      )

  }

}
