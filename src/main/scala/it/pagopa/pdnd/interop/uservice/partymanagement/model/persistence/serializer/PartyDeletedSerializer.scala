package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyDeleted
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1._

import java.io.NotSerializableException

class PartyDeletedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 10001

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val PartyDeletedManifest: String = classOf[PartyDeleted].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case PartyDeleted(party) =>
      v1.events
        .PartyDeletedV1(
          ProtobufSerializer
            .to(party)
            .getOrElse(
              throw new NotSerializableException(
                s"Unable to handle manifest: [[$PartyDeletedManifest]], currentVersion: [[$currentVersion]] "
              )
            )
        )
        .toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {

    manifest.split('|').toList match {
      case PartyDeletedManifest :: `version1` :: Nil =>
        fromBytes(v1.events.PartyAddedV1, bytes) { msg =>
          PartyDeleted(
            ProtobufDeserializer
              .from(msg.party)
              .getOrElse(
                throw new NotSerializableException(
                  s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
                )
              )
          )
        }
      case _ =>
        throw new NotSerializableException(
          s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
        )

    }

  }

}
