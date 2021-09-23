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
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{PartyRelationshipStatus, Token}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{
  Organization,
  OrganizationSeed,
  Person,
  PersonSeed,
  Relationship,
  Relationships,
  RelationshipsSeed,
  TokenSeed
}
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
    "return 404 if the person does not exist" in {

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/persons/$taxCode1", method = HttpMethods.HEAD, headers = authorization)
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.NotFound
    }

    "create a new person" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(personUuid1)).once()

      val response = prepareTest(personSeed1)

      val body = Await.result(Unmarshal(response.entity).to[Person], Duration.Inf)

      response.status shouldBe StatusCodes.Created

      body shouldBe personExpected1

    }

    "return 200 if the person exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(personUuid2)).once()

      val _ = prepareTest(personSeed2)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/persons/$taxCode2", method = HttpMethods.HEAD, headers = authorization)
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.OK
    }

    "return the person if exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(personUuid3)).once()

      val _ = prepareTest(personSeed3)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/persons/$taxCode3", method = HttpMethods.GET, headers = authorization)
        ),
        Duration.Inf
      )

      val body = Await.result(Unmarshal(response.entity).to[Person], Duration.Inf)

      response.status shouldBe StatusCodes.OK

      body shouldBe personExpected2
    }

    "return 400 if person already exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(personUuid4)).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(personUuid5)).once()

      val _ = prepareTest(personSeed4)

      val data = Await.result(Marshal(personSeed4).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val response = createPerson(data)

      response.status shouldBe StatusCodes.BadRequest

    }
  }

  "Working on organizations" must {
    import OrganizationsPartyApiServiceData._
    "return 404 if the organization does not exist" in {

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/organizations/$institutionId1", method = HttpMethods.HEAD, headers = authorization)
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.NotFound
    }

    "create a new organization" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(orgUuid1)).once()

      val response = prepareTest(orgSeed1)

      val body = Await.result(Unmarshal(response.entity).to[Organization], Duration.Inf)

      response.status shouldBe StatusCodes.Created

      body shouldBe expected1

    }

    "return 200 if the organization exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(orgUuid2)).once()

      val _ = prepareTest(orgSeed2)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/organizations/$institutionId2", method = HttpMethods.HEAD, headers = authorization)
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.OK
    }

    "return the organization if exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(orgUuid3)).once()

      val _ = prepareTest(orgSeed3)

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

      (() => uuidSupplier.get).expects().returning(UUID.fromString(orgUuid4)).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(orgUuid5)).once()

      val _ = prepareTest(orgSeed4)

      val data = Await.result(Marshal(orgSeed4).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val response = createOrganization(data)

      response.status shouldBe StatusCodes.BadRequest

    }
  }

  "Working on relationships" must {
    import RelationshipPartyApiServiceData._

    "return 400 if the party does not exist" in {

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/relationships?from=$taxCode1", method = HttpMethods.GET, headers = authorization)
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.BadRequest
    }

    "return 200 if the relationships do not exist" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid1)).once()

      val personData = Await.result(Marshal(personSeed1).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createPerson(personData)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/relationships?from=$taxCode1", method = HttpMethods.GET, headers = authorization)
        ),
        Duration.Inf
      )

      response.status shouldBe StatusCodes.OK

      val body = Await.result(Unmarshal(response.entity).to[Relationships], Duration.Inf)

      response.status shouldBe StatusCodes.OK

      body shouldBe rlExpected1
    }

    "create a new relationship" in {

      (() => uuidSupplier.get).expects().returning(UUID.randomUUID()).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid1)).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid2)).once()

      val response = prepareTest(personSeed = personSeed1, organizationSeed = orgSeed1, relationshipSeed = rlSeed1)

      response.status shouldBe StatusCodes.Created

    }

    "return the relationship if exists" in {
      (() => uuidSupplier.get).expects().returning(UUID.randomUUID()).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid3)).once()
      (() => uuidSupplier.get).expects().returning(rlExpected2.items.head.id).once()

      val _ = prepareTest(personSeed = personSeed2, organizationSeed = orgSeed2, relationshipSeed = rlSeed2)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$url/relationships?from=${personSeed2.taxCode}",
            method = HttpMethods.GET,
            headers = authorization
          )
        ),
        Duration.Inf
      )

      val body = Await.result(Unmarshal(response.entity).to[Relationships], Duration.Inf)

      response.status shouldBe StatusCodes.OK

      body shouldBe rlExpected2
    }

    "return 400 if relationship already exists" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid5)).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid6)).once()

      val _ = prepareTest(personSeed = personSeed3, organizationSeed = orgSeed3, relationshipSeed = rlSeed3)

      val data = Await.result(Marshal(rlSeed3).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val response = createRelationship(data)

      response.status shouldBe StatusCodes.BadRequest

    }

    "return the relationship using `to` party" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid7)).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid8)).once()
      (() => uuidSupplier.get).expects().returning(rlExpected3.items.head.id).once()

      (() => uuidSupplier.get).expects().returning(UUID.fromString(uuid9)).twice()
      (() => uuidSupplier.get).expects().returning(rlExpected3.items(1).id).once()

      val _ = prepareTest(personSeed = personSeed4, organizationSeed = orgSeed4, relationshipSeed = rlSeed4)
      val _ = prepareTest(personSeed = personSeed5, organizationSeed = orgSeed4, relationshipSeed = rlSeed5)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$url/relationships?to=${orgSeed4.institutionId}",
            method = HttpMethods.GET,
            headers = authorization
          )
        ),
        Duration.Inf
      )

      val body = Await.result(Unmarshal(response.entity).to[Relationships], Duration.Inf)

      response.status shouldBe StatusCodes.OK

      body shouldBe rlExpected3
    }
  }

  "Working on token" must {
    import TokenApiServiceData._

    "create a token" in {
      (() => uuidSupplier.get).expects().returning(UUID.randomUUID()).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(createTokenUuid0)).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(createTokenUuid1)).once()

      val relationshipResponse = prepareTest(personSeed1, organizationSeed1, relationshipSeed1, relationshipSeed2)

      val relationships = Await.result(Unmarshal(relationshipResponse.entity).to[RelationshipsSeed], Duration.Inf)

      val tokenSeed = TokenSeed(seed = tokenSeedId1, relationships = relationships, "checksum")

      val tokenData = Await.result(Marshal(tokenSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val response = createToken(tokenData)

      response.status shouldBe StatusCodes.Created
    }

    "consume a token" in {
      (() => uuidSupplier.get).expects().returning(UUID.fromString(createTokenUuid2)).once() //person seed
      (() => uuidSupplier.get).expects().returning(UUID.fromString(createTokenUuid3)).once() //organization seed
      (() => uuidSupplier.get).expects().returning(relationshipId1).once()                   //relationship3 id
      (() => uuidSupplier.get).expects().returning(relationshipId2).once()                   //relationship4 id

      val relationshipResponse = prepareTest(personSeed2, organizationSeed2, relationshipSeed3, relationshipSeed4)

      val _ = Await.result(Unmarshal(relationshipResponse.entity).to[Relationships], Duration.Inf)

      val tokenData = Await.result(Marshal(tokenSeed1).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createToken(tokenData)

      val tokenText = Token.encode(token1)

      val consumedResponse = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/tokens/$tokenText", method = HttpMethods.POST, headers = authorization)
        ),
        Duration.Inf
      )

      consumedResponse.status shouldBe StatusCodes.Created

    }

    "invalidate a token" in {

      (() => uuidSupplier.get).expects().returning(UUID.fromString(createTokenUuid4)).once()
      (() => uuidSupplier.get).expects().returning(UUID.fromString(createTokenUuid5)).once()

      val relationshipResponse = prepareTest(personSeed3, organizationSeed3, relationshipSeed5, relationshipSeed6)

      val _ = Await.result(Unmarshal(relationshipResponse.entity).to[Relationships], Duration.Inf)

      val tokenData = Await.result(Marshal(tokenSeed2).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val _ = createToken(tokenData)

      val tokenText = Token.encode(token2)

      val consumedResponse = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/tokens/$tokenText", method = HttpMethods.DELETE, headers = authorization)
        ),
        Duration.Inf
      )

      consumedResponse.status shouldBe StatusCodes.OK

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$url/relationships?from=${personSeed3.taxCode}",
            method = HttpMethods.GET,
            headers = authorization
          )
        ),
        Duration.Inf
      )

      val body = Await.result(Unmarshal(response.entity).to[Relationships], Duration.Inf)

      body shouldBe Relationships(Seq.empty)

    }
  }

  "Lookup a person by UUID" must {
    "return 404 when the input parameter is not a valid UUID" in {
      //given a random UUID
      val uuid = "YADA-YADA"

      //when looking up for the corresponding person
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/party/person/$uuid", method = HttpMethods.GET, headers = authorization)
        ),
        Duration.Inf
      )

      //then
      response.status shouldBe StatusCodes.NotFound
    }

    "return 404 when the person does not exists" in {
      //given a random UUID
      val uuid = UUID.randomUUID().toString

      //when looking up for the corresponding person
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/party/person/$uuid", method = HttpMethods.GET, headers = authorization)
        ),
        Duration.Inf
      )

      //then
      response.status shouldBe StatusCodes.NotFound
    }

    "return the person payload when it exists" in {
      //given
      val personSeed = PersonSeed(taxCode = "CFTEST", surname = "Soze", name = "Keiser")
      val personId   = UUID.randomUUID()
      (() => uuidSupplier.get).expects().returning(personId).once()
      val personRequestData = Await.result(Marshal(personSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)
      val _                 = createPerson(personRequestData)

      //when
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$url/party/person/${personId.toString}",
            method = HttpMethods.GET,
            headers = authorization
          )
        ),
        Duration.Inf
      )
      val body = Await.result(Unmarshal(response.entity).to[Person], Duration.Inf)

      //then
      response.status shouldBe StatusCodes.OK
      body shouldBe Person(taxCode = "CFTEST", surname = "Soze", name = "Keiser", partyId = personId.toString)
    }
  }

  "Lookup an organization by UUID" must {
    "return 404 when the input parameter is not a valid UUID" in {
      //given a random UUID
      val uuid = "YADA-YADA"

      //when looking up for the corresponding organization
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/party/organization/$uuid", method = HttpMethods.GET, headers = authorization)
        ),
        Duration.Inf
      )

      //then
      response.status shouldBe StatusCodes.NotFound
    }

    "return 404 when the organization does not exist" in {
      //given a random UUID
      val uuid = UUID.randomUUID().toString

      //when looking up for the corresponding organization
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/party/organization/$uuid", method = HttpMethods.GET, headers = authorization)
        ),
        Duration.Inf
      )

      //then
      response.status shouldBe StatusCodes.NotFound
    }

    "return the organization payload when it exists" in {
      //given
      val organizationSeed =
        OrganizationSeed("9999", "ACME Corp.", "Duffy", "Duck", "quack@acme.org", Seq.empty)
      val organizationUUID = UUID.randomUUID()
      (() => uuidSupplier.get).expects().returning(organizationUUID).once()
      val organizationRequestData =
        Await.result(Marshal(organizationSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)
      val _ = createOrganization(organizationRequestData)

      //when
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$url/party/organization/${organizationUUID.toString}",
            method = HttpMethods.GET,
            headers = authorization
          )
        ),
        Duration.Inf
      )
      val body = Await.result(Unmarshal(response.entity).to[Organization], Duration.Inf)

      //then
      response.status shouldBe StatusCodes.OK
      body shouldBe
        Organization(
          "9999",
          "ACME Corp.",
          "Duffy",
          "Duck",
          "quack@acme.org",
          partyId = organizationUUID.toString,
          Seq.empty
        )
    }
  }

  "Lookup a relationship by UUID" must {
    "return 400 when the input parameter is not a valid UUID" in {
      //given a random UUID
      val uuid = "YADA-YADA"

      //when looking up for the corresponding organization
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/relationships/$uuid", method = HttpMethods.GET, headers = authorization)
        ),
        Duration.Inf
      )

      //then
      response.status shouldBe StatusCodes.BadRequest
    }

    "return 404 when the relationship does not exist" in {
      //given a random UUID
      val uuid = UUID.randomUUID().toString

      //when looking up for the corresponding organization
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/relationships/$uuid", method = HttpMethods.GET, headers = authorization)
        ),
        Duration.Inf
      )

      //then
      response.status shouldBe StatusCodes.NotFound
    }

    "return the organization payload when it exists" in {
      import RelationshipPartyApiServiceData._

      //given

      val uuid1 = UUID.randomUUID()
      val uuid2 = UUID.randomUUID()
      val relationshipId = UUID.randomUUID()
      (() => uuidSupplier.get).expects().returning(uuid1).once()
      (() => uuidSupplier.get).expects().returning(uuid2).once()
      (() => uuidSupplier.get).expects().returning(relationshipId).once()

      val _ = prepareTest(personSeed = personSeed6, organizationSeed = orgSeed5, relationshipSeed = rlSeed6)

      //when
      val response = Await.result(
        Http().singleRequest(
          HttpRequest(uri = s"$url/relationships/${relationshipId.toString}", method = HttpMethods.GET, headers = authorization)
        ),
        Duration.Inf
      )

      //then
      response.status shouldBe StatusCodes.OK
      val body = Await.result(Unmarshal(response.entity).to[Relationship], Duration.Inf)
      body shouldBe
        Relationship(
          id = relationshipId,
          from = personSeed6.taxCode,
          to = orgSeed5.institutionId,
          role = rlSeed6.role,
          platformRole = rlSeed6.platformRole,
          status = PartyRelationshipStatus.Pending.toString
        )
    }
  }
}
