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

final case class PersonPartyApiServiceSpec(uuidSupplier: UUIDSupplier)
    extends AnyWordSpec
    with TestSuite
    with Matchers
    with MockFactory {

  import PersonPartyApiServiceSpec._

  "Working on person" must {
    "return 404 if the person does not exists" in {

      val response = Await.result(
        Http().singleRequest(HttpRequest(uri = s"$url/persons/$taxCode1", method = HttpMethods.HEAD)),
        Duration.Inf
      )

      response.status mustBe StatusCodes.NotFound
    }

    "create a new person" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid1)).once()

      val data     = Await.result(Marshal(seed1).to[MessageEntity].map(_.dataBytes), Duration.Inf)
      val response = createPerson(data)

      val body = Await.result(Unmarshal(response.entity).to[Person], Duration.Inf)

      response.status mustBe StatusCodes.Created

      body mustBe expected1

    }

    "return 200 if the person exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid2)).once()

      val data = Await.result(Marshal(seed2).to[MessageEntity].map(_.dataBytes), Duration.Inf)
      val _    = createPerson(data)

      val response = Await.result(
        Http().singleRequest(HttpRequest(uri = s"$url/persons/$taxCode2", method = HttpMethods.HEAD)),
        Duration.Inf
      )

      response.status mustBe StatusCodes.OK
    }

    "return the person if exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid3)).once()

      val data = Await.result(Marshal(seed3).to[MessageEntity].map(_.dataBytes), Duration.Inf)
      val _    = createPerson(data)

      val response = Await.result(
        Http().singleRequest(HttpRequest(uri = s"$url/persons/$taxCode3", method = HttpMethods.GET)),
        Duration.Inf
      )

      val body = Await.result(Unmarshal(response.entity).to[Person], Duration.Inf)

      response.status mustBe StatusCodes.OK

      body mustBe expected2
    }

    "return 400 if person already exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid4)).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid5)).once()

      val data = Await.result(Marshal(seed4).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createPerson(data)

      val response = createPerson(data)

      response.status mustBe StatusCodes.BadRequest

    }
  }

}

object PersonPartyApiServiceSpec {
  final lazy val uuid1 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9215"
  final lazy val uuid2 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9216"
  final lazy val uuid3 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9217"
  final lazy val uuid4 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9218"
  final lazy val uuid5 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9281"

  final lazy val taxCode1 = "RSSMRA75L01H501A"
  final lazy val taxCode2 = "RSSMRA75L01H501B"
  final lazy val taxCode3 = "RSSMRA75L01H501C"
  final lazy val taxCode4 = "RSSMRA75L01H501D"

  final lazy val seed1 = PersonSeed(taxCode = taxCode1, surname = "Doe", name = "Joe")
  final lazy val seed2 = PersonSeed(taxCode = taxCode2, surname = "Durden", name = "Tyler")
  final lazy val seed3 = PersonSeed(taxCode = taxCode3, surname = "Soze", name = "Keiser")
  final lazy val seed4 = PersonSeed(taxCode = taxCode4, surname = "Plissken", name = "Snake")

  final lazy val expected1 = Person(taxCode = taxCode1, surname = "Doe", name = "Joe", partyId = uuid1)
  final lazy val expected2 = Person(taxCode = taxCode3, surname = "Soze", name = "Keiser", partyId = uuid3)

}
