package it.pagopa.pdnd.interop.uservice.partymanagement

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsonMarshaller
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import it.pagopa.pdnd.interop.uservice.partymanagement.api.impl._
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.{classicActorSystem, executionContext}
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.service.UUIDSupplier
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._

@SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.NonUnitStatements"))
final case class OrganizationsPartyApiServiceSpec(uuidSupplier: UUIDSupplier)
    extends AnyWordSpec
    with TestSuite
    with Matchers
    with MockFactory {

  import OrganizationsPartyApiServiceSpec._

  "Working on organizations" must {
    "return 404 if the organization does not exists" in {

      val response = Await.result(
        Http().singleRequest(HttpRequest(uri = s"$url/organizations/$institutionId1", method = HttpMethods.HEAD)),
        Duration.Inf
      )

      response.status mustBe StatusCodes.NotFound
    }

    "create a new organization" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid1)).once()

      val data = Await.result(Marshal(seed1).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val response = createOrganization(data)

      val body = Await.result(Unmarshal(response.entity).to[Organization], Duration.Inf)

      response.status mustBe StatusCodes.Created

      body mustBe expected1

    }

    "return 200 if the organization exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid2)).once()

      val data = Await.result(Marshal(seed2).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createOrganization(data)

      val response = Await.result(
        Http().singleRequest(HttpRequest(uri = s"$url/organizations/$institutionId2", method = HttpMethods.HEAD)),
        Duration.Inf
      )

      response.status mustBe StatusCodes.OK
    }

    "return the organization if exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid3)).once()

      val data = Await.result(Marshal(seed3).to[MessageEntity].map(_.dataBytes), Duration.Inf)
      val _    = createOrganization(data)

      val response = Await.result(
        Http().singleRequest(HttpRequest(uri = s"$url/organizations/$institutionId3", method = HttpMethods.GET)),
        Duration.Inf
      )

      val body = Await.result(Unmarshal(response.entity).to[Organization], Duration.Inf)

      response.status mustBe StatusCodes.OK

      body mustBe expected3
    }

    "return 400 if organization already exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid4)).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid5)).once()

      val data     = Await.result(Marshal(seed4).to[MessageEntity].map(_.dataBytes), Duration.Inf)
      val _        = createOrganization(data)
      val response = createOrganization(data)

      response.status mustBe StatusCodes.BadRequest

    }
  }

}

object OrganizationsPartyApiServiceSpec {

  lazy final val uuid1 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9210"
  lazy final val uuid2 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9211"
  lazy final val uuid3 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9212"
  lazy final val uuid4 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9213"
  lazy final val uuid5 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9231"

  lazy final val institutionId1 = "id1"
  lazy final val institutionId2 = "id2"
  lazy final val institutionId3 = "id3"
  lazy final val institutionId4 = "id4"

  lazy final val seed1 = OrganizationSeed(institutionId1, "Institutions One", "John Doe", "mail1@mail.org", Seq.empty)
  lazy final val seed2 =
    OrganizationSeed(institutionId2, "Institutions Two", "Tyler Durden", "mail2@mail.org", Seq.empty)
  lazy final val seed3 =
    OrganizationSeed(institutionId3, "Institutions Three", "Kaiser Soze", "mail3@mail.org", Seq.empty)
  lazy final val seed4 =
    OrganizationSeed(institutionId4, "Institutions Four", "Snake Plissken", "mail4@mail.org", Seq.empty)

  lazy final val expected1 = Organization(
    institutionId = institutionId1,
    description = "Institutions One",
    manager = "John Doe",
    digitalAddress = "mail1@mail.org",
    partyId = uuid1,
    attributes = Seq.empty
  )
  lazy final val expected3 = Organization(
    institutionId = institutionId3,
    description = "Institutions Three",
    manager = "Kaiser Soze",
    digitalAddress = "mail3@mail.org",
    partyId = uuid3,
    attributes = Seq.empty
  )

}
