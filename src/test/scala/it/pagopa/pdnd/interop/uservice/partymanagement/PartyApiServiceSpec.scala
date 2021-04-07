package it.pagopa.pdnd.interop.uservice.partymanagement

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsonMarshaller
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.http.scaladsl.unmarshalling.Unmarshal
import it.pagopa.pdnd.interop.uservice.partymanagement.api.impl.{PartyApiMarshallerImpl, PartyApiServiceImpl, _}
import it.pagopa.pdnd.interop.uservice.partymanagement.api.{HealthApi, PartyApi, PartyApiMarshaller, PartyApiService}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.{
  Authenticator,
  classicActorSystem,
  executionContext
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyPersistentBehavior
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.server.Controller
import it.pagopa.pdnd.interop.uservice.partymanagement.service.UUIDSupplier
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PartyApiServiceSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with MockFactory {

  val partyApiMarshaller: PartyApiMarshaller = new PartyApiMarshallerImpl

  val url: String = "http://localhost:18088/pdnd-interop-uservice-party-management/0.0.1"

  var controller: Option[Controller]                 = None
  var bindServer: Option[Future[Http.ServerBinding]] = None

  val uuidSupplier: UUIDSupplier = mock[UUIDSupplier]

  override def beforeAll(): Unit = {

    val wrappingDirective: Directive1[Unit] = SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
    val commander                           = ActorSystem(PartyPersistentBehavior(), "pdnd-interop-uservice-party-management")
    val partyApiService: PartyApiService    = new PartyApiServiceImpl(commander, uuidSupplier)

    val partyApi: PartyApi =
      new PartyApi(partyApiService, partyApiMarshaller, wrappingDirective)

    val healthApi: HealthApi = mock[HealthApi]

    controller = Some(new Controller(healthApi, partyApi))

    controller foreach { controller =>
      bindServer = Some(
        Http()
          .newServerAt("0.0.0.0", 18088)
          .bind(controller.routes)
      )

      Await.result(bindServer.get, 100.seconds)
    }

  }

  override def afterAll(): Unit = {

    bindServer.foreach(_.foreach(_.unbind()))

  }

  "Working on organizations" must {
    "return 404 if the organization does not exists" in {

      val response = Await.result(
        Http().singleRequest(HttpRequest(uri = s"$url/organizations/organizationId", method = HttpMethods.HEAD)),
        Duration.Inf
      )

      response.status must be(StatusCodes.NotFound)
    }

    "create a new organization" in {

      val organizationSeed = OrganizationSeed("id1", "Institutions One", "John Doe", "mail1@mail.org")

      (() => uuidSupplier.get).expects().returning(UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9210")).once()

      val data = Await.result(Marshal(organizationSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$url/organizations",
            method = HttpMethods.POST,
            entity = HttpEntity(ContentTypes.`application/json`, data)
          )
        ),
        Duration.Inf
      )

      val body = Await.result(Unmarshal(response.entity).to[Organization], Duration.Inf)

      response.status must be(StatusCodes.Created)

      body must be(
        Organization(
          institutionId = "id1",
          description = "Institutions One",
          manager = "John Doe",
          digitalAddress = "mail1@mail.org",
          partyId = "27f8dce0-0a5b-476b-9fdd-a7a658eb9210"
        )
      )

    }

    "return 200 if the organization exists" in {

      val response = Await.result(
        Http().singleRequest(HttpRequest(uri = s"$url/organizations/id1", method = HttpMethods.HEAD)),
        Duration.Inf
      )

      response.status must be(StatusCodes.OK)
    }

    "return the organization if exists" in {

      val response = Await.result(
        Http().singleRequest(HttpRequest(uri = s"$url/organizations/id1", method = HttpMethods.GET)),
        Duration.Inf
      )

      val body = Await.result(Unmarshal(response.entity).to[Organization], Duration.Inf)

      response.status must be(StatusCodes.OK)

      body must be(
        Organization(
          institutionId = "id1",
          description = "Institutions One",
          manager = "John Doe",
          digitalAddress = "mail1@mail.org",
          partyId = "27f8dce0-0a5b-476b-9fdd-a7a658eb9210"
        )
      )
    }

    "return 400 if organization already exists" in {

      val organizationSeed = OrganizationSeed("id1", "Institutions One", "John Doe", "mail1@mail.org")

      (() => uuidSupplier.get).expects().returning(UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9210")).once()

      val data = Await.result(Marshal(organizationSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$url/organizations",
            method = HttpMethods.POST,
            entity = HttpEntity(ContentTypes.`application/json`, data)
          )
        ),
        Duration.Inf
      )

      response.status must be(StatusCodes.BadRequest)

    }
  }

  "Working on person" must {
    "return 404 if the person does not exists" in {

      val response = Await.result(
        Http().singleRequest(HttpRequest(uri = s"$url/persons/personId", method = HttpMethods.HEAD)),
        Duration.Inf
      )

      response.status must be(StatusCodes.NotFound)
    }

    "create a new person" in {

      val personSeed = PersonSeed(taxCode = "RSSMRA75L01H501A", surname = "Doe", name = "Joe")

      (() => uuidSupplier.get).expects().returning(UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9211")).once()

      val data = Await.result(Marshal(personSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$url/persons",
            method = HttpMethods.POST,
            entity = HttpEntity(ContentTypes.`application/json`, data)
          )
        ),
        Duration.Inf
      )

      val body = Await.result(Unmarshal(response.entity).to[Person], Duration.Inf)

      response.status must be(StatusCodes.Created)

      body must be(
        Person(
          taxCode = "RSSMRA75L01H501A",
          surname = "Doe",
          name = "Joe",
          partyId = "27f8dce0-0a5b-476b-9fdd-a7a658eb9211"
        )
      )

    }

    "return 200 if the person exists" in {

      val response = Await.result(
        Http().singleRequest(HttpRequest(uri = s"$url/persons/RSSMRA75L01H501A", method = HttpMethods.HEAD)),
        Duration.Inf
      )

      response.status must be(StatusCodes.OK)
    }

    "return the person if exists" in {

      val response = Await.result(
        Http().singleRequest(HttpRequest(uri = s"$url/persons/RSSMRA75L01H501A", method = HttpMethods.GET)),
        Duration.Inf
      )

      val body = Await.result(Unmarshal(response.entity).to[Person], Duration.Inf)

      response.status must be(StatusCodes.OK)

      body must be(
        Person(
          taxCode = "RSSMRA75L01H501A",
          surname = "Doe",
          name = "Joe",
          partyId = "27f8dce0-0a5b-476b-9fdd-a7a658eb9211"
        )
      )
    }

    "return 400 if person already exists" in {

      val personSeed = PersonSeed(taxCode = "RSSMRA75L01H501A", surname = "Doe", name = "Joe")

      (() => uuidSupplier.get).expects().returning(UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9211")).once()

      val data = Await.result(Marshal(personSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$url/persons",
            method = HttpMethods.POST,
            entity = HttpEntity(ContentTypes.`application/json`, data)
          )
        ),
        Duration.Inf
      )

      response.status must be(StatusCodes.BadRequest)

    }
  }

  "Working on relationship" must {
    "return 404 if the relationship does not exists" in {

      val response = Await.result(
        Http().singleRequest(HttpRequest(uri = s"$url/relationships/personId", method = HttpMethods.GET)),
        Duration.Inf
      )

      response.status must be(StatusCodes.NotFound)
    }

    "create a new relationship" in {
      val relationShipSeed = RelationShipSeed(
        from = "RSSMRA75L01H501A",
        to = "id1",
        role = Role("Manager")
      )

      val data = Await.result(Marshal(relationShipSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$url/relationships",
            method = HttpMethods.POST,
            entity = HttpEntity(ContentTypes.`application/json`, data)
          )
        ),
        Duration.Inf
      )

      val body = Await.result(Unmarshal(response.entity).to[RelationShip], Duration.Inf)

      response.status must be(StatusCodes.Created)

      body must be(
        RelationShip(taxCode = "RSSMRA75L01H501A", institutionId = "id1", role = "Manager", status = "Pending")
      )

    }

    "return the relationship if exists" in {

      val response = Await.result(
        Http().singleRequest(HttpRequest(uri = s"$url/relationships/RSSMRA75L01H501A", method = HttpMethods.GET)),
        Duration.Inf
      )

      val body = Await.result(Unmarshal(response.entity).to[RelationShip], Duration.Inf)

      response.status must be(StatusCodes.OK)

      body must be(
        RelationShip(taxCode = "RSSMRA75L01H501A", institutionId = "id1", role = "Manager", status = "Pending")
      )
    }

    "return 400 if relationship already exists" in {

      val personSeed = RelationShipSeed(
        from = "27f8dce0-0a5b-476b-9fdd-a7a658eb9211",
        to = "27f8dce0-0a5b-476b-9fdd-a7a658eb9210",
        role = Role("Manager")
      )

      val data = Await.result(Marshal(personSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$url/relationships",
            method = HttpMethods.POST,
            entity = HttpEntity(ContentTypes.`application/json`, data)
          )
        ),
        Duration.Inf
      )

      response.status must be(StatusCodes.BadRequest)

    }
  }

}
