package it.pagopa.pdnd.interop.uservice.partymanagement

import akka.actor
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{Cluster, Join}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, MessageEntity, StatusCodes}
import akka.http.scaladsl.server.directives.{AuthenticationDirective, SecurityDirectives}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.pdnd.interop.uservice.partymanagement.api.impl.{PartyApiMarshallerImpl, PartyApiServiceImpl, _}
import it.pagopa.pdnd.interop.uservice.partymanagement.api.{HealthApi, PartyApi, PartyApiMarshaller, PartyApiService}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.Authenticator
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Organization, Person, RelationShips, TokenSeed}
import it.pagopa.pdnd.interop.uservice.partymanagement.server.Controller
import it.pagopa.pdnd.interop.uservice.partymanagement.server.impl.Main
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object PartyApiServiceSpec {

  val testData: Config = ConfigFactory.parseString(s"""
      akka.actor.provider = cluster

      akka.remote.classic.netty.tcp.port = 0
      akka.remote.artery.canonical.port = 0
      akka.remote.artery.canonical.hostname = 127.0.0.1

      akka.cluster.jmx.multi-mbeans-in-same-jvm = on

      akka.cluster.sharding.number-of-shards = 10

      akka.coordinated-shutdown.terminate-actor-system = off
      akka.coordinated-shutdown.run-by-actor-system-terminate = off
      akka.coordinated-shutdown.run-by-jvm-shutdown-hook = off
      akka.cluster.run-coordinated-shutdown-when-down = off
    """)

  val config: Config = ConfigFactory
    .parseResourcesAnySyntax("test")
    .withFallback(testData)

}

@SuppressWarnings(
  Array("org.wartremover.warts.Var", "org.wartremover.warts.Null", "org.wartremover.warts.OptionPartial")
)
class PartyApiServiceSpec extends ScalaTestWithActorTestKit(PartyApiServiceSpec.config) with AnyWordSpecLike {

  val partyApiMarshaller: PartyApiMarshaller = new PartyApiMarshallerImpl

  var controller: Option[Controller]                 = None
  var bindServer: Option[Future[Http.ServerBinding]] = None

  val sharding: ClusterSharding = ClusterSharding(system)

  val httpSystem: ActorSystem[Any] =
    ActorSystem(Behaviors.ignore[Any], name = system.name, config = system.settings.config)
  implicit val executionContext: ExecutionContextExecutor = httpSystem.executionContext
  implicit val classicSystem: actor.ActorSystem           = httpSystem.classicSystem

  override def beforeAll(): Unit = {

    val persistentEntity = Main.buildPersistentEntity()

    Cluster(system).manager ! Join(Cluster(system).selfMember.address)

    sharding.init(persistentEntity)

    val wrappingDirective: AuthenticationDirective[Seq[(String, String)]] =
      SecurityDirectives.authenticateOAuth2("SecurityRealm", Authenticator)

    val partyApiService: PartyApiService =
      new PartyApiServiceImpl(system, sharding, persistentEntity, uuidSupplier)

    val partyApi: PartyApi =
      new PartyApi(partyApiService, partyApiMarshaller, wrappingDirective)

    val healthApi: HealthApi = mock[HealthApi]

    controller = Some(new Controller(healthApi, partyApi)(classicSystem))

    controller foreach { controller =>
      bindServer = Some(
        Http()
          .newServerAt("0.0.0.0", 8088)
          .bind(controller.routes)
      )

      Await.result(bindServer.get, 100.seconds)
    }

  }

  override def afterAll(): Unit = {

    println("****** Cleaning resources ********")
    bindServer.foreach(_.foreach(_.unbind()))
    ActorTestKit.shutdown(httpSystem, 5.seconds)
    super.afterAll()
    println("Resources cleaned")

  }

  "Working on person" must {
    import PersonPartyApiServiceData._
    "return 404 if the person does not exists" in {

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/persons/$taxCode1", method = HttpMethods.HEAD, headers = authorization)
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.NotFound
    }

    "create a new person" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid1)).once()

      val data     = Await.result(Marshal(seed1).to[MessageEntity].map(_.dataBytes), Duration.Inf)
      val response = createPerson(data)

      val body = Await.result(Unmarshal(response.entity).to[Person], Duration.Inf)

      response.status shouldBe StatusCodes.Created

      body shouldBe expected1

    }

    "return 200 if the person exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid2)).once()

      val data = Await.result(Marshal(seed2).to[MessageEntity].map(_.dataBytes), Duration.Inf)
      val _    = createPerson(data)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/persons/$taxCode2", method = HttpMethods.HEAD, headers = authorization)
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.OK
    }

    "return the person if exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid3)).once()

      val data = Await.result(Marshal(seed3).to[MessageEntity].map(_.dataBytes), Duration.Inf)
      val _    = createPerson(data)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/persons/$taxCode3", method = HttpMethods.GET, headers = authorization)
        ),
        Duration.Inf
      )

      val body = Await.result(Unmarshal(response.entity).to[Person], Duration.Inf)

      response.status shouldBe StatusCodes.OK

      body shouldBe expected2
    }

    "return 400 if person already exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid4)).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid5)).once()

      val data = Await.result(Marshal(seed4).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createPerson(data)

      val response = createPerson(data)

      response.status shouldBe StatusCodes.BadRequest

    }
  }

  "Working on organizations" must {
    import OrganizationsPartyApiServiceData._
    "return 404 if the organization does not exists" in {

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/organizations/$institutionId1", method = HttpMethods.HEAD, headers = authorization)
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.NotFound
    }

    "create a new organization" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid1)).once()

      val data = Await.result(Marshal(seed1).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val response = createOrganization(data)

      val body = Await.result(Unmarshal(response.entity).to[Organization], Duration.Inf)

      response.status shouldBe StatusCodes.Created

      body shouldBe expected1

    }

    "return 200 if the organization exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid2)).once()

      val data = Await.result(Marshal(seed2).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createOrganization(data)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/organizations/$institutionId2", method = HttpMethods.HEAD, headers = authorization)
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.OK
    }

    "return the organization if exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid3)).once()

      val data = Await.result(Marshal(seed3).to[MessageEntity].map(_.dataBytes), Duration.Inf)
      val _    = createOrganization(data)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/organizations/$institutionId3", method = HttpMethods.GET, headers = authorization)
        ),
        Duration.Inf
      )

      val body = Await.result(Unmarshal(response.entity).to[Organization], Duration.Inf)

      response.status shouldBe StatusCodes.OK

      body shouldBe expected3
    }

    "return 400 if organization already exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid4)).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid5)).once()

      val data     = Await.result(Marshal(seed4).to[MessageEntity].map(_.dataBytes), Duration.Inf)
      val _        = createOrganization(data)
      val response = createOrganization(data)

      response.status shouldBe StatusCodes.BadRequest

    }
  }

  "Working on relationships" must {
    import RelationshipPartyApiServiceData._

    "return 400 if the party does not exists" in {

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/relationships/$taxCode1", method = HttpMethods.GET, headers = authorization)
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.BadRequest
    }

    "return 200 with if the relationships do not exist" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid1)).once()

      val personData = Await.result(Marshal(personSeed1).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createPerson(personData)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/relationships/$taxCode1", method = HttpMethods.GET, headers = authorization)
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.OK

      val body = Await.result(Unmarshal(response.entity).to[RelationShips], Duration.Inf)

      response.status shouldBe StatusCodes.OK

      body shouldBe expected0
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

      response.status shouldBe StatusCodes.Created

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
        Http().singleRequest(
          HttpRequest(
            uri = s"$url/relationships/${personSeed2.taxCode}",
            method = HttpMethods.GET,
            headers = authorization
          )
        ),
        Duration.Inf
      )
      val body = Await.result(Unmarshal(response.entity).to[RelationShips], Duration.Inf)

      response.status shouldBe StatusCodes.OK

      body shouldBe expected1
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

      response.status shouldBe StatusCodes.BadRequest

    }
  }

  "Working on token" must {
    import TokenApiServiceData._

    "create a token" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid0)).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid1)).once()

      val personData = Await.result(Marshal(personSeed1).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createPerson(personData)

      val organizationData = Await.result(Marshal(organizationSeed1).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createOrganization(organizationData)

      val data1 = Await.result(Marshal(seed1).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createRelationShip(data1)

      val data2 = Await.result(Marshal(seed2).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createRelationShip(data2)

      val response3 = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/relationships/${seed1.from}", method = HttpMethods.GET, headers = authorization)
        ),
        Duration.Inf
      )

      val relationships = Await.result(Unmarshal(response3.entity).to[RelationShips], Duration.Inf)

      val tokenSeed = TokenSeed(seed = tokenSeed1, relationShips = relationships, "checksum")

      val tokenData = Await.result(Marshal(tokenSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val response = createToken(tokenData)

      response.status shouldBe StatusCodes.Created
    }

  }
}
