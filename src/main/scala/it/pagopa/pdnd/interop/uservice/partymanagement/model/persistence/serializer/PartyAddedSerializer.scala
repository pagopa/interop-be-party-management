package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyAdded
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1._

import java.io.NotSerializableException

class PartyAddedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 10000

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val PartyAddedManifest: String = classOf[PartyAdded].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case PartyAdded(party) =>
      v1.events
        .PartyAddedV1(
          ProtobufSerializer
            .to(party)
            .getOrElse(
              throw new NotSerializableException(
                s"Unable to handle manifest: [[$PartyAddedManifest]], currentVersion: [[$currentVersion]] "
              )
            )
        )
        .toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {

    manifest.split('|').toList match {
      case PartyAddedManifest :: `version1` :: Nil =>
        fromBytes(v1.events.PartyAddedV1, bytes) { msg =>
          PartyAdded(
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
