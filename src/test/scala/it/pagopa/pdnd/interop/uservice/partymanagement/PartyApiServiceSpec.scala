package it.pagopa.pdnd.interop.uservice.partymanagement

import akka.actor
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{Cluster, Join}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.directives.{AuthenticationDirective, SecurityDirectives}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.pdnd.interop.uservice.partymanagement.api.impl.{PartyApiMarshallerImpl, PartyApiServiceImpl, _}
import it.pagopa.pdnd.interop.uservice.partymanagement.api.{HealthApi, PartyApi, PartyApiMarshaller, PartyApiService}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.Authenticator
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{PartyRelationshipStatus, Token}
import it.pagopa.pdnd.interop.uservice.partymanagement.server.Controller
import it.pagopa.pdnd.interop.uservice.partymanagement.server.impl.Main
import it.pagopa.pdnd.interop.uservice.partymanagement.service.FileManager
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object PartyApiServiceSpec {
  //setting up file manager properties

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
    .parseResourcesAnySyntax("application-test")
    .withFallback(testData)

  def fileManagerType: String = config.getString("uservice-party-management.storage.type")
}

class PartyApiServiceSpec extends ScalaTestWithActorTestKit(PartyApiServiceSpec.config) with AnyWordSpecLike {

  val partyApiMarshaller: PartyApiMarshaller = new PartyApiMarshallerImpl

  var controller: Option[Controller]                 = None
  var bindServer: Option[Future[Http.ServerBinding]] = None

  val sharding: ClusterSharding = ClusterSharding(system)

  val httpSystem: ActorSystem[Any] =
    ActorSystem(Behaviors.ignore[Any], name = system.name, config = system.settings.config)
  implicit val executionContext: ExecutionContextExecutor = httpSystem.executionContext
  implicit val classicSystem: actor.ActorSystem           = httpSystem.classicSystem
  val fileManager: FileManager                            = FileManager.getConcreteImplementation(PartyApiServiceSpec.fileManagerType).get

  override def beforeAll(): Unit = {

    val persistentEntity = Main.buildPersistentEntity()

    Cluster(system).manager ! Join(Cluster(system).selfMember.address)

    sharding.init(persistentEntity)

    val wrappingDirective: AuthenticationDirective[Seq[(String, String)]] =
      SecurityDirectives.authenticateOAuth2("SecurityRealm", Authenticator)

    val partyApiService: PartyApiService =
      new PartyApiServiceImpl(system, sharding, persistentEntity, uuidSupplier, fileManager)

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

      val nonExistingId = UUID.randomUUID()

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/persons/${nonExistingId.toString}",
              method = HttpMethods.HEAD,
              headers = authorization
            )
          )
          .futureValue

      response.status shouldBe StatusCodes.NotFound
    }

    "create a new person" in {

      val response = prepareTest(personSeed1)

      val body = Unmarshal(response.entity).to[Person].futureValue

      response.status shouldBe StatusCodes.Created

      body shouldBe personExpected1

    }

    "return 200 if the person exists" in {

      val _ = prepareTest(personSeed2)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/persons/${personUuid2.toString}",
              method = HttpMethods.HEAD,
              headers = authorization
            )
          )
          .futureValue

      response.status shouldBe StatusCodes.OK
    }

    "return the person if exists" in {

      val _ = prepareTest(personSeed3)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/persons/${personUuid3.toString}",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      val body = Unmarshal(response.entity).to[Person].futureValue

      response.status shouldBe StatusCodes.OK

      body shouldBe personExpected2
    }

    "return 400 if person already exists" in {

      val seed = PersonSeed(id = personUuid4)
      val _    = prepareTest(seed)

      val data = Marshal(seed).to[MessageEntity].map(_.dataBytes).futureValue

      val response = createPerson(data)

      response.status shouldBe StatusCodes.BadRequest

    }
  }

  "Working on organizations" must {
    import OrganizationsPartyApiServiceData._
    "return 404 if the organization does not exist" in {

      val nonExistingUuid = UUID.randomUUID()

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/organizations/${nonExistingUuid.toString}",
              method = HttpMethods.HEAD,
              headers = authorization
            )
          )
          .futureValue

      response.status shouldBe StatusCodes.NotFound
    }

    "create a new organization" in {

      (() => uuidSupplier.get).expects().returning(orgUuid1).once()

      val response = prepareTest(orgSeed1)

      val body = Unmarshal(response.entity).to[Organization].futureValue

      response.status shouldBe StatusCodes.Created

      body shouldBe expected1

    }

    "return 200 if the organization exists" in {

      (() => uuidSupplier.get).expects().returning(orgUuid2).once()

      val _ = prepareTest(orgSeed2)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/organizations/${orgUuid2.toString}",
              method = HttpMethods.HEAD,
              headers = authorization
            )
          )
          .futureValue

      response.status shouldBe StatusCodes.OK
    }

    "return the organization if exists" in {

      (() => uuidSupplier.get).expects().returning(orgUuid3).once()

      val _ = prepareTest(orgSeed3)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/organizations/${orgUuid3.toString}",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      val body = Unmarshal(response.entity).to[Organization].futureValue

      response.status shouldBe StatusCodes.OK

      body shouldBe expected3
    }

    "return 400 if organization already exists" in {

      (() => uuidSupplier.get).expects().returning(orgUuid4).once()

      val _ = prepareTest(orgSeed4)

      val data = Marshal(orgSeed4).to[MessageEntity].map(_.dataBytes).futureValue

      val response = createOrganization(data)

      response.status shouldBe StatusCodes.BadRequest

    }
  }

  "Working on relationships" must {
    import RelationshipPartyApiServiceData._

    "return 200 if the relationships do not exist" in {

      val uuid = UUID.randomUUID()
      val seed = PersonSeed(uuid)

      val personData = Marshal(seed).to[MessageEntity].map(_.dataBytes).futureValue

      val _ = createPerson(personData)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships?from=${uuid.toString}",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      response.status shouldBe StatusCodes.OK

      val body = Unmarshal(response.entity).to[Relationships].futureValue

      response.status shouldBe StatusCodes.OK

      body shouldBe Relationships(Seq.empty)
    }

    "create a new relationship" in {

      val personUuid    = UUID.randomUUID()
      val orgUuid       = UUID.randomUUID()
      val relUuid       = UUID.randomUUID()
      val institutionId = randomString()

      val personSeed = PersonSeed(personUuid)
      val orgSeed    = OrganizationSeed(institutionId, None, "Institutions One", "mail1@mail.org", "fiscalCode", Seq.empty)
      val rlSeed     = RelationshipSeed(from = personUuid, to = orgUuid, role = "Manager", "admin")

      (() => uuidSupplier.get).expects().returning(orgUuid).once()
      (() => uuidSupplier.get).expects().returning(relUuid).once()

      val response = prepareTest(personSeed = personSeed, organizationSeed = orgSeed, relationshipSeed = rlSeed)

      response.status shouldBe StatusCodes.Created

    }

    "return the relationship if exists" in {
      val personUuid    = UUID.randomUUID()
      val orgUuid       = UUID.randomUUID()
      val relUuid       = UUID.randomUUID()
      val institutionId = randomString()

      val personSeed = PersonSeed(personUuid)
      val orgSeed    = OrganizationSeed(institutionId, None, "Institutions One", "mail1@mail.org", "fiscalCode", Seq.empty)
      val rlSeed     = RelationshipSeed(from = personUuid, to = orgUuid, role = "Manager", "admin")

      val rlExpected = Relationships(
        Seq(
          Relationship(
            id = relUuid,
            from = personUuid,
            to = orgUuid,
            role = "Manager",
            platformRole = "admin",
            status = "Pending",
            filePath = None,
            fileName = None,
            contentType = None
          )
        )
      )

      (() => uuidSupplier.get).expects().returning(orgUuid).once()
      (() => uuidSupplier.get).expects().returning(rlExpected.items.head.id).once()

      val _ = prepareTest(personSeed = personSeed, organizationSeed = orgSeed, relationshipSeed = rlSeed)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships?from=${personUuid.toString}",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      val body = Unmarshal(response.entity).to[Relationships].futureValue

      response.status shouldBe StatusCodes.OK

      body shouldBe rlExpected
    }

    "return 400 if relationship already exists" in {

      val personUuid    = UUID.randomUUID()
      val orgUuid       = UUID.randomUUID()
      val relUuid       = UUID.randomUUID()
      val institutionId = randomString()

      val personSeed = PersonSeed(personUuid)
      val orgSeed    = OrganizationSeed(institutionId, None, "Institutions One", "mail1@mail.org", "fiscalCode", Seq.empty)
      val rlSeed     = RelationshipSeed(from = personUuid, to = orgUuid, role = "Manager", "admin")

      (() => uuidSupplier.get).expects().returning(orgUuid).once()
      (() => uuidSupplier.get).expects().returning(relUuid).once()

      val _ = prepareTest(personSeed = personSeed, organizationSeed = orgSeed, relationshipSeed = rlSeed)

      val data = Marshal(rlSeed).to[MessageEntity].map(_.dataBytes).futureValue

      val response = createRelationship(data)

      response.status shouldBe StatusCodes.BadRequest

    }

    "return the relationship using `to` party" in {

      val personUuid1   = UUID.randomUUID()
      val personUuid2   = UUID.randomUUID()
      val orgUuid       = UUID.randomUUID()
      val relUuid1      = UUID.randomUUID()
      val relUuid2      = UUID.randomUUID()
      val institutionId = randomString()

      val personSeed1    = PersonSeed(personUuid1)
      val personSeed2    = PersonSeed(personUuid2)
      val orgSeed        = OrganizationSeed(institutionId, None, "Institutions One", "mail1@mail.org", "fiscalCode", Seq.empty)
      val rlSeedAdmin    = RelationshipSeed(from = personUuid1, to = orgUuid, role = "Manager", "admin")
      val rlSeedDelegate = RelationshipSeed(from = personUuid2, to = orgUuid, role = "Delegate", "admin")

      val rlExpected = Relationships(
        Seq(
          Relationship(
            id = relUuid1,
            from = personUuid1,
            to = orgUuid,
            role = "Manager",
            platformRole = "admin",
            status = "Pending",
            filePath = None,
            fileName = None,
            contentType = None
          ),
          Relationship(
            id = relUuid2,
            from = personUuid2,
            to = orgUuid,
            role = "Delegate",
            platformRole = "admin",
            status = "Pending",
            filePath = None,
            fileName = None,
            contentType = None
          )
        )
      )

      (() => uuidSupplier.get).expects().returning(orgUuid).once()
      (() => uuidSupplier.get).expects().returning(relUuid1).once()
      (() => uuidSupplier.get).expects().returning(relUuid2).once()

      val _ = prepareTest(personSeed = personSeed1, organizationSeed = orgSeed, relationshipSeed = rlSeedAdmin)
      val _ = prepareTest(personSeed = personSeed2, organizationSeed = orgSeed, relationshipSeed = rlSeedDelegate)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships?to=${orgUuid.toString}",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      val body = Unmarshal(response.entity).to[Relationships].futureValue

      response.status shouldBe StatusCodes.OK

      body.items should contain theSameElementsAs rlExpected.items
    }

    "filter relationships by platform role" in {

      val personUuid1   = UUID.randomUUID()
      val personUuid2   = UUID.randomUUID()
      val orgUuid       = UUID.randomUUID()
      val relUuid1      = UUID.randomUUID()
      val relUuid2      = UUID.randomUUID()
      val institutionId = randomString()

      val personSeed1    = PersonSeed(personUuid1)
      val personSeed2    = PersonSeed(personUuid2)
      val orgSeed        = OrganizationSeed(institutionId, None, "Institutions One", "mail1@mail.org", "fiscalCode", Seq.empty)
      val rlSeedAdmin    = RelationshipSeed(from = personUuid1, to = orgUuid, role = "Manager", "admin")
      val rlSeedSecurity = RelationshipSeed(from = personUuid2, to = orgUuid, role = "Delegate", "security")

      val rlExpected = Relationships(
        Seq(
          Relationship(
            id = relUuid2,
            from = personUuid2,
            to = orgUuid,
            role = "Delegate",
            platformRole = "security",
            status = "Pending",
            filePath = None,
            fileName = None,
            contentType = None
          )
        )
      )

      (() => uuidSupplier.get).expects().returning(orgUuid).once()
      (() => uuidSupplier.get).expects().returning(relUuid1).once()

      (() => uuidSupplier.get).expects().returning(relUuid2).once()

      val _ = prepareTest(personSeed = personSeed1, organizationSeed = orgSeed, relationshipSeed = rlSeedAdmin)
      val _ = prepareTest(personSeed = personSeed2, organizationSeed = orgSeed, relationshipSeed = rlSeedSecurity)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships?to=${orgUuid.toString}&platformRole=security",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      val body = Unmarshal(response.entity).to[Relationships].futureValue

      response.status shouldBe StatusCodes.OK

      body.items should contain theSameElementsAs rlExpected.items
    }
  }

  "Suspending relationship" must {
    import RelationshipPartyApiServiceData._

    "succeed" in {
      val personUuid    = UUID.randomUUID()
      val orgUuid       = UUID.randomUUID()
      val institutionId = randomString()
      val personSeed    = PersonSeed(id = personUuid)
      val organizationSeed =
        OrganizationSeed(institutionId, None, "Institutions One", "mail1@mail.org", "fiscalCode", Seq.empty)
      val relationshipSeed = RelationshipSeed(from = personUuid, to = orgUuid, role = "Manager", "admin")
      val relationshipId   = UUID.randomUUID()

      (() => uuidSupplier.get).expects().returning(orgUuid).once()        // Create organization
      (() => uuidSupplier.get).expects().returning(relationshipId).once() // Create relationship

      val _ =
        prepareTest(personSeed = personSeed, organizationSeed = organizationSeed, relationshipSeed = relationshipSeed)

      confirmRelationshipWithToken(relationshipSeed)

      val suspensionResponse =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships/${relationshipId.toString}/suspend",
              method = HttpMethods.POST,
              headers = authorization
            )
          )
          .futureValue

      suspensionResponse.status shouldBe StatusCodes.NoContent

      val relationshipResponse =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships/${relationshipId.toString}",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      relationshipResponse.status shouldBe StatusCodes.OK
      val updatedRelationship = Unmarshal(relationshipResponse.entity).to[Relationship].futureValue
      updatedRelationship.status shouldBe PartyRelationshipStatus.Suspended.toString

    }

    "fail if relationship does not exist" in {
      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships/non-existing-relationship/suspend",
              method = HttpMethods.POST,
              headers = authorization
            )
          )
          .futureValue

      response.status shouldBe StatusCodes.NotFound
    }

  }

  "Activating relationship" must {
    import RelationshipPartyApiServiceData._

    "succeed" in {
      val personUuid    = UUID.randomUUID()
      val orgUuid       = UUID.randomUUID()
      val institutionId = randomString()
      val personSeed    = PersonSeed(id = personUuid)
      val organizationSeed =
        OrganizationSeed(institutionId, None, "Institutions One", "mail1@mail.org", "fiscalCode", Seq.empty)
      val relationshipSeed = RelationshipSeed(from = personUuid, to = orgUuid, role = "Manager", "admin")
      val relationshipId   = UUID.randomUUID()

      (() => uuidSupplier.get).expects().returning(orgUuid).once()        // Create organization
      (() => uuidSupplier.get).expects().returning(relationshipId).once() // Create relationship

      val _ =
        prepareTest(personSeed = personSeed, organizationSeed = organizationSeed, relationshipSeed = relationshipSeed)

      confirmRelationshipWithToken(relationshipSeed)

      // First suspend the relationship
      val suspensionResponse =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships/${relationshipId.toString}/suspend",
              method = HttpMethods.POST,
              headers = authorization
            )
          )
          .futureValue

      suspensionResponse.status shouldBe StatusCodes.NoContent

      // Then activate the relationship
      val activationResponse =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships/${relationshipId.toString}/activate",
              method = HttpMethods.POST,
              headers = authorization
            )
          )
          .futureValue

      activationResponse.status shouldBe StatusCodes.NoContent

      val relationshipResponse =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships/${relationshipId.toString}",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      relationshipResponse.status shouldBe StatusCodes.OK
      val updatedRelationship = Unmarshal(relationshipResponse.entity).to[Relationship].futureValue
      updatedRelationship.status shouldBe PartyRelationshipStatus.Active.toString

    }

    "fail if relationship does not exist" in {
      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships/non-existing-relationship/activate",
              method = HttpMethods.POST,
              headers = authorization
            )
          )
          .futureValue

      response.status shouldBe StatusCodes.NotFound
    }

  }

  "Deleting relationship" must {
    import RelationshipPartyApiServiceData._

    "succeed" in {
      val personUuid    = UUID.randomUUID()
      val orgUuid       = UUID.randomUUID()
      val institutionId = randomString()
      val personSeed    = PersonSeed(id = personUuid)
      val organizationSeed =
        OrganizationSeed(institutionId, None, "Institutions One", "mail1@mail.org", "fiscalCode", Seq.empty)
      val relationshipSeed = RelationshipSeed(from = personUuid, to = orgUuid, role = "Manager", "admin")
      val relationshipId   = UUID.randomUUID()

      (() => uuidSupplier.get).expects().returning(orgUuid).once()        // Create organization
      (() => uuidSupplier.get).expects().returning(relationshipId).once() // Create relationship

      val _ =
        prepareTest(personSeed = personSeed, organizationSeed = organizationSeed, relationshipSeed = relationshipSeed)

      confirmRelationshipWithToken(relationshipSeed)

      val deleteResponse =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships/${relationshipId.toString}",
              method = HttpMethods.DELETE,
              headers = authorization
            )
          )
          .futureValue

      deleteResponse.status shouldBe StatusCodes.NoContent

      val relationshipResponse =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships/${relationshipId.toString}",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      relationshipResponse.status shouldBe StatusCodes.OK
      val updatedRelationship = Unmarshal(relationshipResponse.entity).to[Relationship].futureValue
      updatedRelationship.status shouldBe PartyRelationshipStatus.Deleted.toString

    }

    "fail if relationship does not exist" in {
      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships/${UUID.randomUUID()}",
              method = HttpMethods.DELETE,
              headers = authorization
            )
          )
          .futureValue

      response.status shouldBe StatusCodes.NotFound
    }

  }

  "Working on token" must {
    import TokenApiServiceData._

    "create a token" in {

      (() => uuidSupplier.get).expects().returning(orgId1).once()           // organization seed
      (() => uuidSupplier.get).expects().returning(createTokenUuid0).once() // relationship1 id
      (() => uuidSupplier.get).expects().returning(createTokenUuid1).once() // relationship2 id

      val relationshipResponse = prepareTest(personSeed1, organizationSeed1, relationshipSeed1, relationshipSeed2)

      val relationships = Unmarshal(relationshipResponse.entity).to[RelationshipsSeed].futureValue

      val tokenSeed = TokenSeed(seed = tokenSeedId1, relationships = relationships, "checksum")

      val tokenData = Marshal(tokenSeed).to[MessageEntity].map(_.dataBytes).futureValue

      val response = createToken(tokenData)

      response.status shouldBe StatusCodes.Created
    }

    "consume a token" in {
      (() => uuidSupplier.get).expects().returning(orgId2).once()          // organization seed
      (() => uuidSupplier.get).expects().returning(relationshipId1).once() // relationship3 id
      (() => uuidSupplier.get).expects().returning(relationshipId2).once() // relationship4 id

      val relationshipResponse = prepareTest(personSeed2, organizationSeed2, relationshipSeed3, relationshipSeed4)

      val _ = Unmarshal(relationshipResponse.entity).to[Relationships].futureValue

      val tokenData = Marshal(tokenSeed1).to[MessageEntity].map(_.dataBytes).futureValue

      val _ = createToken(tokenData)

      val tokenText = Token.encode(token1)

      val formData = Multipart.FormData
        .fromFile("doc", MediaTypes.`application/octet-stream`, file = writeToTempFile("hello world"), 100000)
        .toEntity

      val consumedResponse =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/tokens/$tokenText",
              method = HttpMethods.POST,
              headers = multipart,
              entity = formData
            )
          )
          .futureValue
      consumedResponse.status shouldBe StatusCodes.Created
    }

    "invalidate a token" in {

      (() => uuidSupplier.get).expects().returning(orgId3).once()
      (() => uuidSupplier.get).expects().returning(relationshipId3).once()
      (() => uuidSupplier.get).expects().returning(relationshipId4).once()

      val relationshipResponse = prepareTest(personSeed3, organizationSeed3, relationshipSeed5, relationshipSeed6)

      val _ = Unmarshal(relationshipResponse.entity).to[Relationships].futureValue

      val tokenData = Marshal(tokenSeed2).to[MessageEntity].map(_.dataBytes).futureValue

      val _ = createToken(tokenData)

      val tokenText = Token.encode(token2)

      val consumedResponse =
        Http()
          .singleRequest(
            HttpRequest(uri = s"$url/tokens/$tokenText", method = HttpMethods.DELETE, headers = authorization)
          )
          .futureValue

      consumedResponse.status shouldBe StatusCodes.OK

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships?from=${personId3.toString}",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      val body = Unmarshal(response.entity).to[Relationships].futureValue

      body.items.map(_.status) should contain only PartyRelationshipStatus.Rejected.stringify

    }
  }

  "Lookup a relationship by UUID" must {
    "return 400 when the input parameter is not a valid UUID" in {
      //given a random UUID
      val uuid = "YADA-YADA"

      //when looking up for the corresponding organization
      val response =
        Http()
          .singleRequest(
            HttpRequest(uri = s"$url/relationships/$uuid", method = HttpMethods.GET, headers = authorization)
          )
          .futureValue

      //then
      response.status shouldBe StatusCodes.BadRequest
    }

    "return 404 when the relationship does not exist" in {
      //given a random UUID

      val uuid = UUID.randomUUID().toString

      //when looking up for the corresponding organization
      val response =
        Http()
          .singleRequest(
            HttpRequest(uri = s"$url/relationships/$uuid", method = HttpMethods.GET, headers = authorization)
          )
          .futureValue

      //then
      response.status shouldBe StatusCodes.NotFound
    }

    "return the organization payload when it exists" in {
      import RelationshipPartyApiServiceData._

      //given

      val personUuid     = UUID.randomUUID()
      val orgUuid        = UUID.randomUUID()
      val relationshipId = UUID.randomUUID()
      val institutionId  = randomString()

      val personSeed = PersonSeed(personUuid)
      val orgSeed    = OrganizationSeed(institutionId, None, "Institutions One", "mail1@mail.org", "fiscalCode", Seq.empty)
      val rlSeed     = RelationshipSeed(from = personUuid, to = orgUuid, role = "Manager", "admin")

      (() => uuidSupplier.get).expects().returning(orgUuid).once()
      (() => uuidSupplier.get).expects().returning(relationshipId).once()

      val _ = prepareTest(personSeed = personSeed, organizationSeed = orgSeed, relationshipSeed = rlSeed)

      //when
      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships/${relationshipId.toString}",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      //then
      response.status shouldBe StatusCodes.OK
      val body = Unmarshal(response.entity).to[Relationship].futureValue
      body shouldBe
        Relationship(
          id = relationshipId,
          from = personUuid,
          to = orgUuid,
          role = rlSeed.role,
          platformRole = rlSeed.platformRole,
          status = PartyRelationshipStatus.Pending.toString,
          filePath = None,
          fileName = None,
          contentType = None
        )
    }
  }
}
