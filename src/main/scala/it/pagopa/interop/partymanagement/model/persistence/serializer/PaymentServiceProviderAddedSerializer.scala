package it.pagopa.interop.partymanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.interop.partymanagement.model.persistence.{PaymentServiceProviderAdded}

import java.io.NotSerializableException

class PaymentServiceProviderAddedSerializer extends SerializerWithStringManifest {
  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 600000

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val className: String = classOf[PaymentServiceProviderAdded].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event: PaymentServiceProviderAdded => serialize(event, className, currentVersion)
    case _                                  =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[${manifest(o)}]], currentVersion: [[$currentVersion]] "
      )
  }

}
