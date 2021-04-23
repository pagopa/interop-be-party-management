package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.TokenConsumed
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1._

import java.io.NotSerializableException

class TokenConsumedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 30002

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val TokenConsumedManifest: String = classOf[TokenConsumed].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case TokenConsumed(token) =>
      v1.events
        .TokenConsumedV1(
          ProtobufSerializer
            .to(token)
            .getOrElse(
              throw new NotSerializableException(
                s"Unable to handle manifest: [[$TokenConsumedManifest]], currentVersion: [[$currentVersion]] "
              )
            )
        )
        .toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {

    manifest.split('|').toList match {
      case TokenConsumedManifest :: `version1` :: Nil =>
        fromBytes(v1.events.TokenConsumedV1, bytes) { msg =>
          TokenConsumed(
            ProtobufDeserializer
              .from(msg.token)
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
