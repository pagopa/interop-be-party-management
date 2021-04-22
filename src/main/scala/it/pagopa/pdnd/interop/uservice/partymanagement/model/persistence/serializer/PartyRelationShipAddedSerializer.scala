package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyRelationShipAdded
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1._
import java.io.NotSerializableException

class PartyRelationShipAddedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 20001

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val PartyRelationShipAddedManifest: String = classOf[PartyRelationShipAdded].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case PartyRelationShipAdded(partyRelationShip) =>
      v1.events.PartyRelationShipAddedV1(partyRelationShip.toPartyRelationShipV1).toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {

    manifest.split('|').toList match {
      case PartyRelationShipAddedManifest :: `version1` :: Nil =>
        fromBytes(v1.events.PartyRelationShipAddedV1, bytes) { msg =>
          PartyRelationShipAdded(msg.partyRelationShip.toPartyRelationShip)
        }
      case _ =>
        throw new NotSerializableException(
          s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
        )

    }

  }

}
