package it.pagopa.pdnd.interop.uservice.partymanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.{HttpResponse, MessageEntity}
import it.pagopa.pdnd.interop.uservice.partymanagement.model._

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object OrganizationsPartyApiServiceData {

  lazy final val orgUuid1 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9210")
  lazy final val orgUuid2 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9211")
  lazy final val orgUuid3 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9212")
  lazy final val orgUuid4 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9213")

  lazy final val institutionId1 = "id1"
  lazy final val institutionId2 = "id2"
  lazy final val institutionId3 = "id3"
  lazy final val institutionId4 = "id4"

  lazy final val orgSeed1 = OrganizationSeed(
    institutionId = institutionId1,
    description = "Institutions One",
    digitalAddress = "mail1@mail.org",
    address = "address1",
    zipCode = "zipCode1",
    taxCode = "taxCode",
    products = Set.empty,
    attributes = Seq.empty
  )
  lazy final val orgSeed2 =
    OrganizationSeed(
      institutionId = institutionId2,
      description = "Institutions Two",
      digitalAddress = "mail2@mail.org",
      address = "address2",
      zipCode = "zipCode2",
      taxCode = "taxCode",
      products = Set.empty,
      attributes = Seq.empty
    )
  lazy final val orgSeed3 =
    OrganizationSeed(
      institutionId = institutionId3,
      description = "Institutions Three",
      digitalAddress = "mail3@mail.org",
      address = "address3",
      zipCode = "zipCode3",
      taxCode = "taxCode",
      products = Set.empty,
      attributes = Seq.empty
    )
  lazy final val orgSeed4 =
    OrganizationSeed(
      institutionId = institutionId4,
      description = "Institutions Four",
      digitalAddress = "mail4@mail.org",
      address = "address4",
      zipCode = "zipCode4",
      taxCode = "taxCode",
      products = Set.empty,
      attributes = Seq.empty
    )

  lazy final val expected1 = Organization(
    institutionId = institutionId1,
    description = "Institutions One",
    digitalAddress = "mail1@mail.org",
    address = "address1",
    zipCode = "zipCode1",
    taxCode = "taxCode",
    id = orgUuid1,
    attributes = Seq.empty
  )

  lazy final val expected3 = Organization(
    institutionId = institutionId3,
    description = "Institutions Three",
    digitalAddress = "mail3@mail.org",
    address = "address3",
    zipCode = "zipCode3",
    taxCode = "taxCode",
    id = orgUuid3,
    attributes = Seq.empty
  )

  def prepareTest(
    organizationSeed: OrganizationSeed
  )(implicit as: ActorSystem, mp: Marshaller[OrganizationSeed, MessageEntity], ec: ExecutionContext): HttpResponse = {
    val orgRequestData = Await.result(Marshal(organizationSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    createOrganization(orgRequestData)
  }

}
