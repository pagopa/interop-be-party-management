package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.{OrganizationProductsAdded, PartyAdded}

import java.io.NotSerializableException

class OrganizationProductsAddedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 500000

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val OrganizationProductsAddedManifest: String = classOf[PartyAdded].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event: OrganizationProductsAdded => serialize(event, OrganizationProductsAddedManifest, currentVersion)
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest.split('|').toList match {
    case OrganizationProductsAddedManifest :: `version1` :: Nil =>
      deserialize(v1.events.OrganizationProductsAddedV1, bytes, manifest, version1)
    case _ =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
      )

  }

}
