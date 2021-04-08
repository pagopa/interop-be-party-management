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
final case class RelationshipPartyApiServiceSpec(uuidSupplier: UUIDSupplier)
    extends AnyWordSpec
    with TestSuite
    with Matchers
    with MockFactory {

  import RelationshipPartyApiServiceSpec._

  "Working on relationships" must {

    "return 404 if the relationship does not exists" in {

      val response = Await.result(
        Http().singleRequest(HttpRequest(uri = s"$url/relationships/$uuid1", method = HttpMethods.GET)),
        Duration.Inf
      )

      response.status mustBe StatusCodes.NotFound
    }

    "create a new relationship" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid1)).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid2)).once()

      val personData = Await.result(Marshal(personSeed1).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createPerson(personData)

      val organizationData = Await.result(Marshal(organizationSeed1).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createOrganization(organizationData)

      val data = Await.result(Marshal(seed1).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val response = createRelationShip(data)

      response.status mustBe StatusCodes.Created

    }

    "return the relationship if exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid3)).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid4)).once()

      val personData = Await.result(Marshal(personSeed2).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createPerson(personData)

      val organizationData = Await.result(Marshal(organizationSeed2).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createOrganization(organizationData)

      val data = Await.result(Marshal(seed2).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createRelationShip(data)

      val response = Await.result(
        Http().singleRequest(HttpRequest(uri = s"$url/relationships/$uuid3", method = HttpMethods.GET)),
        Duration.Inf
      )

      val body = Await.result(Unmarshal(response.entity).to[RelationShips], Duration.Inf)

      response.status mustBe StatusCodes.OK

      body mustBe expected1
    }

    "return 400 if relationship already exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid5)).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid6)).once()

      val personData = Await.result(Marshal(personSeed3).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createPerson(personData)

      val organizationData = Await.result(Marshal(organizationSeed3).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createOrganization(organizationData)

      val data = Await.result(Marshal(seed3).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createRelationShip(data)

      val response = createRelationShip(data)

      response.status mustBe StatusCodes.BadRequest

    }
  }

}

object RelationshipPartyApiServiceSpec {

  lazy final val uuid1 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9219"
  lazy final val uuid2 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9220"
  lazy final val uuid3 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9221"
  lazy final val uuid4 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9222"
  lazy final val uuid5 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9223"
  lazy final val uuid6 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9224"

  lazy final val taxCode1 = "RSSMRA75L01H501E"
  lazy final val taxCode2 = "RSSMRA75L01H501F"
  lazy final val taxCode3 = "RSSMRA75L01H501G"

  lazy final val institutionId1 = "id5"
  lazy final val institutionId2 = "id6"
  lazy final val institutionId3 = "id7"

  lazy final val personSeed1 = PersonSeed(taxCode = taxCode1, surname = "Ripley", name = "Ellen")
  lazy final val personSeed2 = PersonSeed(taxCode = taxCode2, surname = "Onizuka", name = "Eikichi")
  lazy final val personSeed3 = PersonSeed(taxCode = taxCode3, surname = "Murphy", name = "Alex")

  lazy final val organizationSeed1 =
    OrganizationSeed(institutionId1, "Institutions Five", "Ellen Ripley", "mail5@mail.org")
  lazy final val organizationSeed2 =
    OrganizationSeed(institutionId2, "Institutions Six", "Eikichi Onizuka", "mail6@mail.org")
  lazy final val organizationSeed3 =
    OrganizationSeed(institutionId3, "Institutions Seven", "Alex Murphy", "mail7@mail.org")

  lazy final val seed1 = RelationShipSeed(from = taxCode1, to = institutionId1, role = Role("Manager"))
  lazy final val seed2 = RelationShipSeed(from = taxCode2, to = institutionId2, role = Role("Manager"))
  lazy final val seed3 = RelationShipSeed(from = taxCode3, to = institutionId3, role = Role("Manager"))

  lazy final val expected1 = RelationShips(
    Seq(RelationShip(taxCode = taxCode2, institutionId = institutionId2, role = "Manager", status = "Pending"))
  )
}
