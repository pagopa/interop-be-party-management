package it.pagopa.interop.partymanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.{HttpResponse, MessageEntity}
import it.pagopa.interop.partymanagement.model._

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object InstitutionsPartyApiServiceData {

  lazy final val institutionUuid1 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9210")
  lazy final val institutionUuid2 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9211")
  lazy final val institutionUuid3 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9212")
  lazy final val institutionUuid4 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9213")

  lazy final val externalId1 = "ext_id1"
  lazy final val originId1   = "origin_id1"
  lazy final val externalId2 = "ext_id2"
  lazy final val originId2   = "origin_id2"
  lazy final val externalId3 = "ext_id3"
  lazy final val originId3   = "origin_id3"
  lazy final val externalId4 = "ext_id4"
  lazy final val originId4   = "origin_id4"

  lazy final val institutionSeed1 = InstitutionSeed(
    externalId = externalId1,
    originId = originId1,
    description = "Institutions One",
    digitalAddress = "mail1@mail.org",
    address = "address1",
    zipCode = "zipCode1",
    taxCode = "taxCode",
    products = Set.empty,
    attributes = Seq.empty,
    origin = "IPA",
    institutionType = Option("PA")
  )
  lazy final val institutionSeed2 =
    InstitutionSeed(
      externalId = externalId2,
      originId = originId2,
      description = "Institutions Two",
      digitalAddress = "mail2@mail.org",
      address = "address2",
      zipCode = "zipCode2",
      taxCode = "taxCode",
      products = Set.empty,
      attributes = Seq.empty,
      origin = "IPA",
      institutionType = Option("PA")
    )
  lazy final val institutionSeed3 =
    InstitutionSeed(
      externalId = externalId3,
      originId = originId3,
      description = "Institutions Three",
      digitalAddress = "mail3@mail.org",
      address = "address3",
      zipCode = "zipCode3",
      taxCode = "taxCode",
      products = Set.empty,
      attributes = Seq.empty,
      origin = "IPA",
      institutionType = Option("PA")
    )
  lazy final val institutionSeed4 =
    InstitutionSeed(
      externalId = externalId4,
      originId = originId4,
      description = "Institutions Four",
      digitalAddress = "mail4@mail.org",
      address = "address4",
      zipCode = "zipCode4",
      taxCode = "taxCode",
      products = Set.empty,
      attributes = Seq.empty,
      origin = "IPA",
      institutionType = Option("PA")
    )

  lazy final val expected1 = Institution(
    externalId = externalId1,
    originId = originId1,
    description = "Institutions One",
    digitalAddress = "mail1@mail.org",
    address = "address1",
    zipCode = "zipCode1",
    taxCode = "taxCode",
    id = institutionUuid1,
    attributes = Seq.empty,
    origin = "IPA",
    institutionType = Option("PA")
  )

  lazy final val expected3 = Institution(
    externalId = externalId3,
    originId = originId3,
    description = "Institutions Three",
    digitalAddress = "mail3@mail.org",
    address = "address3",
    zipCode = "zipCode3",
    taxCode = "taxCode",
    id = institutionUuid3,
    attributes = Seq.empty,
    origin = "IPA",
    institutionType = Option("PA")
  )

  def prepareTest(
    institutionSeed: InstitutionSeed
  )(implicit as: ActorSystem, mp: Marshaller[InstitutionSeed, MessageEntity], ec: ExecutionContext): HttpResponse = {
    val orgRequestData = Await.result(Marshal(institutionSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    createInstitution(orgRequestData)
  }

}
