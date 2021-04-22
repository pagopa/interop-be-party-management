package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyRelationShipDeleted
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1._
import java.io.NotSerializableException

class PartyRelationShipDeletedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 20002

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val PartyRelationShipDeletedManifest: String = classOf[PartyRelationShipDeleted].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case PartyRelationShipDeleted(relationShipId) =>
      v1.events.PartyRelationShipDeletedV1(relationShipId.toPartyRelationShipIdV1).toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {

    manifest.split('|').toList match {
      case PartyRelationShipDeletedManifest :: `version1` :: Nil =>
        fromBytes(v1.events.PartyRelationShipDeletedV1, bytes) { msg =>
          PartyRelationShipDeleted(msg.partyRelationShipId.toPartyRelationShipId)
        }
      case _ =>
        throw new NotSerializableException(
          s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
        )

    }

  }

}
