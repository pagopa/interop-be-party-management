package it.pagopa.interop.partymanagement

import akka.actor
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.cluster.typed.{Cluster, Join}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.directives.{AuthenticationDirective, SecurityDirectives}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.utils.AkkaUtils
import it.pagopa.interop.commons.utils.AkkaUtils.Authenticator
import it.pagopa.interop.partymanagement.api._
import it.pagopa.interop.partymanagement.api.impl.{PartyApiMarshallerImpl, PartyApiServiceImpl, _}
import it.pagopa.interop.partymanagement.model._
import it.pagopa.interop.partymanagement.model.party.Token
import it.pagopa.interop.partymanagement.model.persistence.PartyPersistentBehavior
import it.pagopa.interop.partymanagement.server.Controller
import it.pagopa.interop.partymanagement.server.impl.Main.behaviorFactory
import it.pagopa.interop.partymanagement.service.{InstitutionService, RelationshipService}
import it.pagopa.interop.partymanagement.service.impl.{InstitutionServiceImpl, RelationshipServiceImpl}
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object PartyApiServiceSpec {
  // setting up file manager properties

  final val timestamp = OffsetDateTime.parse("2021-11-23T13:37:00.277147+01:00")

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
    .resolve()

  def fileManagerType: String = config.getString("interop-commons.storage.type")
}

class PartyApiServiceSpec extends ScalaTestWithActorTestKit(PartyApiServiceSpec.config) with AnyWordSpecLike {

  var controller: Option[Controller]                 = None
  var bindServer: Option[Future[Http.ServerBinding]] = None

  val sharding: ClusterSharding = ClusterSharding(system)

  val httpSystem: ActorSystem[Any] =
    ActorSystem(Behaviors.ignore[Any], name = system.name, config = system.settings.config)

  implicit val executionContext: ExecutionContextExecutor = httpSystem.executionContext
  implicit val classicSystem: actor.ActorSystem           = httpSystem.classicSystem

  val fileManager: FileManager = FileManager.getConcreteImplementation(PartyApiServiceSpec.fileManagerType).get

  override def beforeAll(): Unit = {

    val persistentEntity = Entity(PartyPersistentBehavior.TypeKey)(behaviorFactory(offsetDateTimeSupplier))

    Cluster(system).manager ! Join(Cluster(system).selfMember.address)

    sharding.init(persistentEntity)

    val relationshipService: RelationshipService = new RelationshipServiceImpl(system, sharding, persistentEntity)
    val institutionService: InstitutionService   = new InstitutionServiceImpl(system, sharding, persistentEntity)

    val wrappingDirective: AuthenticationDirective[Seq[(String, String)]] =
      SecurityDirectives.authenticateOAuth2("SecurityRealm", Authenticator)

    val partyApiService: PartyApiService =
      new PartyApiServiceImpl(
        system = system,
        sharding = sharding,
        entity = persistentEntity,
        uuidSupplier = uuidSupplier,
        offsetDateTimeSupplier = offsetDateTimeSupplier,
        relationshipService,
        institutionService
      )

    val partyApi: PartyApi =
      new PartyApi(partyApiService, PartyApiMarshallerImpl, wrappingDirective)

    val externalApiService: ExternalApiService =
      new ExternalApiServiceImpl(system = system, sharding = sharding, entity = persistentEntity)

    val externalApi: ExternalApi =
      new ExternalApi(externalApiService, ExternalApiMarshallerImpl, wrappingDirective)

    val publicApiService: PublicApiService =
      new PublicApiServiceImpl(
        system = system,
        sharding = sharding,
        entity = persistentEntity,
        fileManager = fileManager
      )

    val publicApi: PublicApi =
      new PublicApi(
        publicApiService,
        PublicApiMarshallerImpl,
        SecurityDirectives.authenticateOAuth2("public", AkkaUtils.PassThroughAuthenticator)
      )

    val healthApi: HealthApi = mock[HealthApi]

    controller = Some(
      new Controller(health = healthApi, party = partyApi, external = externalApi, public = publicApi)(classicSystem)
    )

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
    bindServer.foreach(_.foreach(_.unbind()))
    ActorTestKit.shutdown(httpSystem, 5.seconds)
    super.afterAll()
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

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once()

      val response = prepareTest(personSeed1)

      val body = Unmarshal(response.entity).to[Person].futureValue

      response.status shouldBe StatusCodes.Created

      body shouldBe personExpected1

    }

    "return 200 if the person exists" in {

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once()

      prepareTest(personSeed2)

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

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once()

      prepareTest(personSeed3)

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

    "return 409 if person already exists" in {

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once()

      val seed = PersonSeed(id = personUuid4)
      val _    = prepareTest(seed)

      val data = Marshal(seed).to[MessageEntity].map(_.dataBytes).futureValue

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once()

      val response = createPerson(data)

      response.status shouldBe StatusCodes.Conflict

    }
  }

  "Working on institutions" must {
    import InstitutionsPartyApiServiceData._
    "return 404 if the institution does not exist" in {

      val nonExistingUuid = UUID.randomUUID()

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/institutions/${nonExistingUuid.toString}",
              method = HttpMethods.HEAD,
              headers = authorization
            )
          )
          .futureValue

      response.status shouldBe StatusCodes.NotFound
    }

    "create a new institution" in {

      (() => uuidSupplier.get).expects().returning(institutionUuid1).once()

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once()

      val response = prepareTest(institutionSeed1)

      val body = Unmarshal(response.entity).to[Institution].futureValue

      response.status shouldBe StatusCodes.Created

      body shouldBe expected1

    }

    "return 200 if the institution exists" in {

      (() => uuidSupplier.get).expects().returning(institutionUuid2).once()

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once()

      prepareTest(institutionSeed2)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/institutions/${institutionUuid2.toString}",
              method = HttpMethods.HEAD,
              headers = authorization
            )
          )
          .futureValue

      response.status shouldBe StatusCodes.OK
    }

    "return the institution if exists" in {

      (() => uuidSupplier.get).expects().returning(institutionUuid3).once()

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once()

      prepareTest(institutionSeed3)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/institutions/${institutionUuid3.toString}",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      val body = Unmarshal(response.entity).to[Institution].futureValue

      response.status shouldBe StatusCodes.OK

      body shouldBe expected3
    }

    "return 409 if institution already exists" in {

      (() => uuidSupplier.get).expects().returning(institutionUuid4).once()

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once()

      prepareTest(institutionSeed4)

      val data = Marshal(institutionSeed4).to[MessageEntity].map(_.dataBytes).futureValue

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once()

      val response = createInstitution(data)

      response.status shouldBe StatusCodes.Conflict

    }

    "allow to update of an existing institution" in {
      val uuid        = UUID.randomUUID()
      val institution = institutionSeed1.copy(externalId = "newExternalId", originId = "newOriginId")

      (() => uuidSupplier.get).expects().returning(uuid).once()

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once()

      prepareTest(institution)

      val updated =
        expected1.copy(
          id = uuid,
          externalId = institution.externalId,
          originId = institution.originId,
          description = s"UPDATED_${expected1.description}"
        )

      val data = Marshal(updated).to[MessageEntity].map(_.dataBytes).futureValue

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once()

      val response = updateInstitution(uuid.toString, data)

      response.status shouldBe StatusCodes.OK

      val body = Unmarshal(response.entity).to[Institution].futureValue

      body shouldBe updated
    }

    "not allow to update NOT existing institution" in {
      val data = Marshal(expected1).to[MessageEntity].map(_.dataBytes).futureValue

      val response = updateInstitution(UUID.randomUUID.toString, data)

      response.status shouldBe StatusCodes.NotFound
    }

    "return 400 when updating an existing institution changing its externalId and block the update" in {
      val uuid        = UUID.randomUUID()
      val institution = institutionSeed1.copy(externalId = randomString(), originId = randomString())
      val expected    = expected1.copy(id = uuid, externalId = institution.externalId, originId = institution.originId)

      (() => uuidSupplier.get).expects().returning(uuid).once()

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once()

      prepareTest(institution)

      val updatedExternalId =
        expected.copy(externalId = s"UPDATED_${expected.externalId}", address = s"UPDATED_${expected.address}")

      val dataExternalId = Marshal(updatedExternalId).to[MessageEntity].map(_.dataBytes).futureValue

      val responseExternalId = updateInstitution(uuid.toString, dataExternalId)

      responseExternalId.status shouldBe StatusCodes.BadRequest

      val updatedOriginId =
        expected.copy(originId = s"UPDATED_${expected.originId}", address = s"UPDATED_${expected.address}")

      val dataOriginId = Marshal(updatedOriginId).to[MessageEntity].map(_.dataBytes).futureValue

      val responseOrigin = updateInstitution(uuid.toString, dataOriginId)

      responseOrigin.status shouldBe StatusCodes.BadRequest

      val responseGET =
        Http()
          .singleRequest(
            HttpRequest(uri = s"$url/institutions/${uuid.toString}", method = HttpMethods.GET, headers = authorization)
          )
          .futureValue

      responseGET.status shouldBe StatusCodes.OK

      val bodyGET = Unmarshal(responseGET.entity).to[Institution].futureValue

      bodyGET shouldBe expected
    }
  }

  "Working on relationships" must {
    import RelationshipPartyApiServiceData._

    "return 200 if the relationships do not exist" in {

      val uuid = UUID.randomUUID()
      val seed = PersonSeed(uuid)

      val personData = Marshal(seed).to[MessageEntity].map(_.dataBytes).futureValue

      createPerson(personData)

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

      val personUuid      = UUID.randomUUID()
      val institutionUuid = UUID.randomUUID()
      val relUuid         = UUID.randomUUID()
      val externalId      = randomString()
      val originId        = randomString()

      val personSeed      = PersonSeed(personUuid)
      val institutionSeed =
        InstitutionSeed(
          externalId = externalId,
          originId = originId,
          description = "Institutions One",
          digitalAddress = "mail1@mail.org",
          address = "address",
          zipCode = "zipCode",
          taxCode = "taxCode",
          products = Option(Map.empty[String, InstitutionProduct]),
          attributes = Seq.empty,
          origin = "IPA",
          institutionType = Option("PA")
        )
      val rlSeed          =
        RelationshipSeed(
          from = personUuid,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          RelationshipProductSeed(id = "p1", role = "admin"),
          pricingPlan = Option("PRICING_PLAN"),
          billing = Option(Billing("VATNUMBER", "RECIPIENTCODE", Option(true))),
          institutionUpdate = Option(
            InstitutionUpdate(
              Option("PAOVERRIDE"),
              Option("DESCRIPTIONOVERRIDE"),
              Option("MAILOVERRIDE"),
              Option("ADDRESSOVERRIDE"),
              Option("TAXCODEOVERRIDE")
            )
          )
        )

      (() => uuidSupplier.get).expects().returning(institutionUuid).once() // Create institution
      (() => uuidSupplier.get).expects().returning(relUuid).once()         // Create relationship

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship

      val response = prepareTest(personSeed = personSeed, institutionSeed = institutionSeed, relationshipSeed = rlSeed)

      response.status shouldBe StatusCodes.Created

    }

    "return the relationship if exists" in {
      val personUuid      = UUID.randomUUID()
      val institutionUuid = UUID.randomUUID()
      val relUuid         = UUID.randomUUID()
      val externalId      = randomString()
      val originId        = randomString()

      val personSeed      = PersonSeed(personUuid)
      val institutionSeed = InstitutionSeed(
        externalId = externalId,
        originId = originId,
        description = "Institutions One",
        digitalAddress = "mail1@mail.org",
        address = "address",
        zipCode = "zipCode",
        taxCode = "taxCode",
        products = Option(Map.empty[String, InstitutionProduct]),
        attributes = Seq.empty,
        origin = "IPA",
        institutionType = Option("PA")
      )
      val rlSeed          =
        RelationshipSeed(
          from = personUuid,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          RelationshipProductSeed(id = "p1", role = "admin"),
          pricingPlan = Option("PRICING_PLAN"),
          billing = Option(Billing("VATNUMBER", "RECIPIENTCODE", Option(true))),
          institutionUpdate = Option(
            InstitutionUpdate(
              Option("PAOVERRIDE"),
              Option("DESCRIPTIONOVERRIDE"),
              Option("MAILOVERRIDE"),
              Option("ADDRESSOVERRIDE"),
              Option("TAXCODEOVERRIDE")
            )
          )
        )

      val rlExpected = Relationships(
        Seq(
          Relationship(
            id = relUuid,
            from = personUuid,
            to = institutionUuid,
            role = PartyRole.MANAGER,
            product = RelationshipProduct(id = "p1", role = "admin", createdAt = timestampValid),
            state = RelationshipState.PENDING,
            filePath = None,
            fileName = None,
            contentType = None,
            createdAt = timestampValid,
            updatedAt = None,
            pricingPlan = Option("PRICING_PLAN"),
            billing = Option(Billing("VATNUMBER", "RECIPIENTCODE", Option(true))),
            institutionUpdate = Option(
              InstitutionUpdate(
                Option("PAOVERRIDE"),
                Option("DESCRIPTIONOVERRIDE"),
                Option("MAILOVERRIDE"),
                Option("ADDRESSOVERRIDE"),
                Option("TAXCODEOVERRIDE")
              )
            )
          )
        )
      )

      (() => uuidSupplier.get).expects().returning(institutionUuid).once()          // Create institution
      (() => uuidSupplier.get).expects().returning(rlExpected.items.head.id).once() // Create relationship

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship

      prepareTest(personSeed = personSeed, institutionSeed = institutionSeed, relationshipSeed = rlSeed)

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

    "return 409 if relationship already exists" in {

      val personUuid      = UUID.randomUUID()
      val institutionUuid = UUID.randomUUID()
      val relUuid         = UUID.randomUUID()
      val externalId      = randomString()
      val originId        = randomString()

      val personSeed      = PersonSeed(personUuid)
      val institutionSeed =
        InstitutionSeed(
          externalId = externalId,
          originId = originId,
          description = "Institutions One",
          digitalAddress = "mail1@mail.org",
          address = "address",
          zipCode = "zipCode",
          taxCode = "taxCode",
          products = Option(Map.empty[String, InstitutionProduct]),
          attributes = Seq.empty,
          origin = "IPA",
          institutionType = Option("PA")
        )
      val rlSeed          =
        RelationshipSeed(
          from = personUuid,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          RelationshipProductSeed(id = "p1", role = "admin")
        )

      (() => uuidSupplier.get).expects().returning(institutionUuid).once()          // Create institution
      (() => uuidSupplier.get).expects().returning(relUuid).once()                  // Create relationship
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship

      prepareTest(personSeed = personSeed, institutionSeed = institutionSeed, relationshipSeed = rlSeed)

      val data = Marshal(rlSeed).to[MessageEntity].map(_.dataBytes).futureValue

      val response = createRelationship(data)

      response.status shouldBe StatusCodes.Conflict

    }

    "return the relationship using `to` party" in {

      val personUuid1     = UUID.randomUUID()
      val personUuid2     = UUID.randomUUID()
      val institutionUuid = UUID.randomUUID()
      val relUuid1        = UUID.randomUUID()
      val relUuid2        = UUID.randomUUID()
      val externalId      = randomString()
      val originId        = randomString()

      val personSeed1     = PersonSeed(personUuid1)
      val personSeed2     = PersonSeed(personUuid2)
      val institutionSeed =
        InstitutionSeed(
          externalId = externalId,
          originId = originId,
          description = "Institutions One",
          digitalAddress = "mail1@mail.org",
          address = "address",
          zipCode = "zipCode",
          taxCode = "taxCode",
          products = Option(Map.empty[String, InstitutionProduct]),
          attributes = Seq.empty,
          origin = "IPA",
          institutionType = Option("PA")
        )
      val rlSeedAdmin     =
        RelationshipSeed(
          from = personUuid1,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          RelationshipProductSeed(id = "p1", role = "admin")
        )
      val rlSeedDelegate  =
        RelationshipSeed(
          from = personUuid2,
          to = institutionUuid,
          role = PartyRole.DELEGATE,
          RelationshipProductSeed(id = "p1", role = "admin")
        )

      val rlExpected = Relationships(
        Seq(
          Relationship(
            id = relUuid1,
            from = personUuid1,
            to = institutionUuid,
            role = PartyRole.MANAGER,
            product = RelationshipProduct(id = "p1", role = "admin", createdAt = timestampValid),
            state = RelationshipState.PENDING,
            filePath = None,
            fileName = None,
            contentType = None,
            createdAt = timestampValid,
            updatedAt = None
          ),
          Relationship(
            id = relUuid2,
            from = personUuid2,
            to = institutionUuid,
            role = PartyRole.DELEGATE,
            product = RelationshipProduct(id = "p1", role = "admin", createdAt = timestampValid),
            state = RelationshipState.PENDING,
            filePath = None,
            fileName = None,
            contentType = None,
            createdAt = timestampValid,
            updatedAt = None
          )
        )
      )

      (() => uuidSupplier.get).expects().returning(institutionUuid).once() // Create institution
      (() => uuidSupplier.get).expects().returning(relUuid1).once()        // Create relationship1
      (() => uuidSupplier.get).expects().returning(relUuid2).once()        // Create relationship2

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship2

      prepareTest(personSeed = personSeed1, institutionSeed = institutionSeed, relationshipSeed = rlSeedAdmin)
      prepareTest(personSeed = personSeed2, institutionSeed = institutionSeed, relationshipSeed = rlSeedDelegate)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships?to=${institutionUuid.toString}",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      val body = Unmarshal(response.entity).to[Relationships].futureValue

      response.status shouldBe StatusCodes.OK

      body.items should contain theSameElementsAs rlExpected.items
    }

    "filter relationships by product roles" in {

      val personUuid1     = UUID.randomUUID()
      val personUuid2     = UUID.randomUUID()
      val personUuid3     = UUID.randomUUID()
      val institutionUuid = UUID.randomUUID()
      val relUuid1        = UUID.randomUUID()
      val relUuid2        = UUID.randomUUID()
      val relUuid3        = UUID.randomUUID()
      val externalId      = randomString()
      val originId        = randomString()

      val personSeed1     = PersonSeed(personUuid1)
      val personSeed2     = PersonSeed(personUuid2)
      val personSeed3     = PersonSeed(personUuid3)
      val institutionSeed =
        InstitutionSeed(
          externalId = externalId,
          originId = originId,
          description = "Institutions One",
          digitalAddress = "mail1@mail.org",
          address = "address",
          zipCode = "zipCode",
          taxCode = "taxCode",
          products = Option(Map.empty[String, InstitutionProduct]),
          attributes = Seq.empty,
          origin = "IPA",
          institutionType = Option("PA")
        )
      val rlSeedAdmin     =
        RelationshipSeed(
          from = personUuid1,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          RelationshipProductSeed(id = "p1", role = "admin")
        )
      val rlSeedSecurity  = RelationshipSeed(
        from = personUuid2,
        to = institutionUuid,
        role = PartyRole.DELEGATE,
        RelationshipProductSeed(id = "p1", role = "security")
      )
      val rlSeedApi       = RelationshipSeed(
        from = personUuid3,
        to = institutionUuid,
        role = PartyRole.DELEGATE,
        RelationshipProductSeed(id = "p1", role = "api")
      )

      val rlExpected = Relationships(
        Seq(
          Relationship(
            id = relUuid2,
            from = personUuid2,
            to = institutionUuid,
            role = PartyRole.DELEGATE,
            product = RelationshipProduct(id = "p1", role = "security", createdAt = timestampValid),
            state = RelationshipState.PENDING,
            filePath = None,
            fileName = None,
            contentType = None,
            createdAt = timestampValid,
            updatedAt = None
          ),
          Relationship(
            id = relUuid3,
            from = personUuid3,
            to = institutionUuid,
            role = PartyRole.DELEGATE,
            product = RelationshipProduct(id = "p1", role = "api", createdAt = timestampValid),
            state = RelationshipState.PENDING,
            filePath = None,
            fileName = None,
            contentType = None,
            createdAt = timestampValid,
            updatedAt = None
          )
        )
      )

      (() => uuidSupplier.get).expects().returning(institutionUuid).once() // Create institution
      (() => uuidSupplier.get).expects().returning(relUuid1).once()        // Create relationship1
      (() => uuidSupplier.get).expects().returning(relUuid2).once()        // Create relationship2
      (() => uuidSupplier.get).expects().returning(relUuid3).once()        // Create relationship3

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person3
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship3

      prepareTest(personSeed = personSeed1, institutionSeed = institutionSeed, relationshipSeed = rlSeedAdmin)
      prepareTest(personSeed = personSeed2, institutionSeed = institutionSeed, relationshipSeed = rlSeedSecurity)
      prepareTest(personSeed = personSeed3, institutionSeed = institutionSeed, relationshipSeed = rlSeedApi)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships?to=${institutionUuid.toString}&productRoles=security,api",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      val body = Unmarshal(response.entity).to[Relationships].futureValue

      response.status shouldBe StatusCodes.OK

      body.items should contain theSameElementsAs rlExpected.items
    }

    "filter relationships by products" in {

      val personUuid1     = UUID.randomUUID()
      val personUuid2     = UUID.randomUUID()
      val personUuid3     = UUID.randomUUID()
      val institutionUuid = UUID.randomUUID()
      val relUuid1        = UUID.randomUUID()
      val relUuid2        = UUID.randomUUID()
      val relUuid3        = UUID.randomUUID()
      val externalId      = randomString()
      val originId        = randomString()

      val personSeed1     = PersonSeed(personUuid1)
      val personSeed2     = PersonSeed(personUuid2)
      val personSeed3     = PersonSeed(personUuid3)
      val institutionSeed =
        InstitutionSeed(
          externalId = externalId,
          originId = originId,
          description = "Institutions One",
          digitalAddress = "mail1@mail.org",
          address = "address",
          zipCode = "zipCode",
          taxCode = "taxCode",
          products = Option(Map.empty[String, InstitutionProduct]),
          attributes = Seq.empty,
          origin = "IPA",
          institutionType = Option("PA")
        )
      val rlSeedAdmin     =
        RelationshipSeed(
          from = personUuid1,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          RelationshipProductSeed(id = "p1", role = "admin")
        )
      val rlSeedPDND      = RelationshipSeed(
        from = personUuid2,
        to = institutionUuid,
        role = PartyRole.DELEGATE,
        RelationshipProductSeed(id = "PDND", role = "security")
      )
      val rlSeedIO        = RelationshipSeed(
        from = personUuid3,
        to = institutionUuid,
        role = PartyRole.DELEGATE,
        RelationshipProductSeed(id = "IO", role = "security")
      )

      val rlExpected = Relationships(
        Seq(
          Relationship(
            id = relUuid2,
            from = personUuid2,
            to = institutionUuid,
            role = PartyRole.DELEGATE,
            product = RelationshipProduct(id = "PDND", role = "security", createdAt = timestampValid),
            state = RelationshipState.PENDING,
            filePath = None,
            fileName = None,
            contentType = None,
            createdAt = timestampValid,
            updatedAt = None
          ),
          Relationship(
            id = relUuid3,
            from = personUuid3,
            to = institutionUuid,
            role = PartyRole.DELEGATE,
            product = RelationshipProduct(id = "IO", role = "security", createdAt = timestampValid),
            state = RelationshipState.PENDING,
            filePath = None,
            fileName = None,
            contentType = None,
            createdAt = timestampValid,
            updatedAt = None
          )
        )
      )

      (() => uuidSupplier.get).expects().returning(institutionUuid).once() // Create institution
      (() => uuidSupplier.get).expects().returning(relUuid1).once()        // Create relationship1
      (() => uuidSupplier.get).expects().returning(relUuid2).once()        // Create relationship2
      (() => uuidSupplier.get).expects().returning(relUuid3).once()        // Create relationship3

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person3
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship3

      prepareTest(personSeed = personSeed1, institutionSeed = institutionSeed, relationshipSeed = rlSeedAdmin)
      prepareTest(personSeed = personSeed2, institutionSeed = institutionSeed, relationshipSeed = rlSeedPDND)
      prepareTest(personSeed = personSeed3, institutionSeed = institutionSeed, relationshipSeed = rlSeedIO)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships?to=${institutionUuid.toString}&products=PDND,IO",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      val body = Unmarshal(response.entity).to[Relationships].futureValue

      response.status shouldBe StatusCodes.OK

      body.items should contain theSameElementsAs rlExpected.items
    }

    "filter relationships by roles" in {

      val personUuid1     = UUID.randomUUID()
      val personUuid2     = UUID.randomUUID()
      val institutionUuid = UUID.randomUUID()
      val relUuid1        = UUID.randomUUID()
      val relUuid2        = UUID.randomUUID()
      val externalId      = randomString()
      val originId        = randomString()

      val personSeed1     = PersonSeed(personUuid1)
      val personSeed2     = PersonSeed(personUuid2)
      val institutionSeed =
        InstitutionSeed(
          externalId = externalId,
          originId = originId,
          description = "Institutions One",
          digitalAddress = "mail1@mail.org",
          address = "address",
          zipCode = "zipCode",
          taxCode = "taxCode",
          products = Option(Map.empty[String, InstitutionProduct]),
          attributes = Seq.empty,
          origin = "IPA",
          institutionType = Option("PA")
        )
      val rlSeedAdmin     =
        RelationshipSeed(
          from = personUuid1,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          RelationshipProductSeed(id = "p1", role = "admin")
        )
      val rlSeedDelegate  = RelationshipSeed(
        from = personUuid2,
        to = institutionUuid,
        role = PartyRole.DELEGATE,
        RelationshipProductSeed(id = "p1", role = "security")
      )

      val rlExpected = Relationships(
        Seq(
          Relationship(
            id = relUuid2,
            from = personUuid2,
            to = institutionUuid,
            role = PartyRole.DELEGATE,
            product = RelationshipProduct(id = "p1", role = "security", createdAt = timestampValid),
            state = RelationshipState.PENDING,
            filePath = None,
            fileName = None,
            contentType = None,
            createdAt = timestampValid,
            updatedAt = None
          )
        )
      )

      (() => uuidSupplier.get).expects().returning(institutionUuid).once() // Create institution
      (() => uuidSupplier.get).expects().returning(relUuid1).once()        // Create relationship1
      (() => uuidSupplier.get).expects().returning(relUuid2).once()        // Create relationship2

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship2

      prepareTest(personSeed = personSeed1, institutionSeed = institutionSeed, relationshipSeed = rlSeedAdmin)
      prepareTest(personSeed = personSeed2, institutionSeed = institutionSeed, relationshipSeed = rlSeedDelegate)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships?to=${institutionUuid.toString}&roles=DELEGATE",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      val body = Unmarshal(response.entity).to[Relationships].futureValue

      response.status shouldBe StatusCodes.OK

      body.items should contain theSameElementsAs rlExpected.items
    }

    "filter relationships by states" in {

      val personUuid1     = UUID.randomUUID()
      val personUuid2     = UUID.randomUUID()
      val institutionUuid = UUID.randomUUID()
      val relUuid1        = UUID.randomUUID()
      val relUuid2        = UUID.randomUUID()
      val externalId      = randomString()
      val originId        = randomString()

      val personSeed1     = PersonSeed(personUuid1)
      val personSeed2     = PersonSeed(personUuid2)
      val institutionSeed =
        InstitutionSeed(
          externalId = externalId,
          originId = originId,
          description = "Institutions One",
          digitalAddress = "mail1@mail.org",
          address = "address",
          zipCode = "zipCode",
          taxCode = "taxCode",
          products = Option(Map.empty[String, InstitutionProduct]),
          attributes = Seq.empty,
          origin = "IPA",
          institutionType = Option("PA")
        )
      val rlSeedAdmin     =
        RelationshipSeed(
          from = personUuid1,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          RelationshipProductSeed(id = "p1", role = "admin")
        )
      val rlSeedPending   = RelationshipSeed(
        from = personUuid2,
        to = institutionUuid,
        role = PartyRole.DELEGATE,
        RelationshipProductSeed(id = "p1", role = "security")
      )

      val rlExpected = Relationships(
        Seq(
          Relationship(
            id = relUuid1,
            from = personUuid1,
            to = institutionUuid,
            role = PartyRole.MANAGER,
            product = RelationshipProduct(id = "p1", role = "admin", createdAt = timestampValid),
            state = RelationshipState.PENDING,
            filePath = None,
            fileName = None,
            contentType = None,
            createdAt = timestampValid,
            updatedAt = None
          ),
          Relationship(
            id = relUuid2,
            from = personUuid2,
            to = institutionUuid,
            role = PartyRole.DELEGATE,
            product = RelationshipProduct(id = "p1", role = "security", createdAt = timestampValid),
            state = RelationshipState.PENDING,
            filePath = None,
            fileName = None,
            contentType = None,
            createdAt = timestampValid,
            updatedAt = None
          )
        )
      )

      (() => uuidSupplier.get).expects().returning(institutionUuid).once() // Create institution
      (() => uuidSupplier.get).expects().returning(relUuid1).once()        // Create relationship1
      (() => uuidSupplier.get).expects().returning(relUuid2).once()        // Create relationship2

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship2

      prepareTest(personSeed = personSeed1, institutionSeed = institutionSeed, relationshipSeed = rlSeedAdmin)
      prepareTest(personSeed = personSeed2, institutionSeed = institutionSeed, relationshipSeed = rlSeedPending)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships?to=${institutionUuid.toString}&states=PENDING",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      val body = Unmarshal(response.entity).to[Relationships].futureValue

      response.status shouldBe StatusCodes.OK

      body.items should contain theSameElementsAs rlExpected.items
    }

    "filter relationships by all filters" in {

      val personUuid1     = UUID.randomUUID()
      val personUuid2     = UUID.randomUUID()
      val institutionUuid = UUID.randomUUID()
      val relUuid1        = UUID.randomUUID()
      val relUuid2        = UUID.randomUUID()
      val externalId      = randomString()
      val originId        = randomString()

      val personSeed1     = PersonSeed(personUuid1)
      val personSeed2     = PersonSeed(personUuid2)
      val institutionSeed =
        InstitutionSeed(
          externalId = externalId,
          originId = originId,
          description = "Institutions One",
          digitalAddress = "mail1@mail.org",
          address = "address",
          zipCode = "zipCode",
          taxCode = "taxCode",
          products = Option(Map.empty[String, InstitutionProduct]),
          attributes = Seq.empty,
          origin = "IPA",
          institutionType = Option("PA")
        )
      val rlSeedAdmin     =
        RelationshipSeed(
          from = personUuid1,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          RelationshipProductSeed(id = "p1", role = "admin")
        )
      val rlSeedSecurity  = RelationshipSeed(
        from = personUuid2,
        to = institutionUuid,
        role = PartyRole.DELEGATE,
        RelationshipProductSeed(id = "PDND", role = "security")
      )

      val rlExpected = Relationships(
        Seq(
          Relationship(
            id = relUuid2,
            from = personUuid2,
            to = institutionUuid,
            role = PartyRole.DELEGATE,
            product = RelationshipProduct(id = "PDND", role = "security", createdAt = timestampValid),
            state = RelationshipState.PENDING,
            filePath = None,
            fileName = None,
            contentType = None,
            createdAt = timestampValid,
            updatedAt = None
          )
        )
      )

      (() => uuidSupplier.get).expects().returning(institutionUuid).once() // Create institution
      (() => uuidSupplier.get).expects().returning(relUuid1).once()        // Create relationship1
      (() => uuidSupplier.get).expects().returning(relUuid2).once()        // Create relationship2

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship2

      prepareTest(personSeed = personSeed1, institutionSeed = institutionSeed, relationshipSeed = rlSeedAdmin)
      prepareTest(personSeed = personSeed2, institutionSeed = institutionSeed, relationshipSeed = rlSeedSecurity)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri =
                s"$url/relationships?to=${institutionUuid.toString}&products=PDND&productRoles=security&role=DELEGATE&states=PENDING",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      val body = Unmarshal(response.entity).to[Relationships].futureValue

      response.status shouldBe StatusCodes.OK

      body.items should contain theSameElementsAs rlExpected.items
    }

    "not retrieve relationships if not match any filters." in {

      val personUuid1     = UUID.randomUUID()
      val personUuid2     = UUID.randomUUID()
      val institutionUuid = UUID.randomUUID()
      val relUuid1        = UUID.randomUUID()
      val relUuid2        = UUID.randomUUID()
      val externalId      = randomString()
      val originId        = randomString()

      val personSeed1     = PersonSeed(personUuid1)
      val personSeed2     = PersonSeed(personUuid2)
      val institutionSeed =
        InstitutionSeed(
          externalId = externalId,
          originId = originId,
          description = "Institutions One",
          digitalAddress = "mail1@mail.org",
          address = "address",
          zipCode = "zipCode",
          taxCode = "taxCode",
          products = Option(Map.empty[String, InstitutionProduct]),
          attributes = Seq.empty,
          origin = "IPA",
          institutionType = Option("PA")
        )
      val rlSeedAdmin     =
        RelationshipSeed(
          from = personUuid1,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          RelationshipProductSeed(id = "p1", role = "admin")
        )
      val rlSeedSecurity  = RelationshipSeed(
        from = personUuid2,
        to = institutionUuid,
        role = PartyRole.DELEGATE,
        RelationshipProductSeed(id = "p1", role = "security")
      )

      (() => uuidSupplier.get).expects().returning(institutionUuid).once() // Create institution
      (() => uuidSupplier.get).expects().returning(relUuid1).once()        // Create relationship1
      (() => uuidSupplier.get).expects().returning(relUuid2).once()        // Create relationship2

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship2

      prepareTest(personSeed = personSeed1, institutionSeed = institutionSeed, relationshipSeed = rlSeedAdmin)
      prepareTest(personSeed = personSeed2, institutionSeed = institutionSeed, relationshipSeed = rlSeedSecurity)

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri =
                s"$url/relationships?to=${institutionUuid.toString}&products=Interop&productRoles=security&roles=DELEGATE&states=PENDING",
              method = HttpMethods.GET,
              headers = authorization
            )
          )
          .futureValue

      val body = Unmarshal(response.entity).to[Relationships].futureValue

      response.status shouldBe StatusCodes.OK

      body.items shouldBe empty
    }

  }

  "Suspending relationship" must {
    import RelationshipPartyApiServiceData._

    "succeed" in {
      val personUuid       = UUID.randomUUID()
      val institutionUuid  = UUID.randomUUID()
      val externalId       = randomString()
      val originId         = randomString()
      val personSeed       = PersonSeed(id = personUuid)
      val institutionSeed  =
        InstitutionSeed(
          externalId = externalId,
          originId = originId,
          description = "Institutions One",
          digitalAddress = "mail1@mail.org",
          address = "address",
          zipCode = "zipCode",
          taxCode = "taxCode",
          products = Option(Map.empty[String, InstitutionProduct]),
          attributes = Seq.empty,
          origin = "IPA",
          institutionType = Option("PA")
        )
      val relationshipSeed =
        RelationshipSeed(
          from = personUuid,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          RelationshipProductSeed(id = "p1", role = "admin")
        )
      val relationship     =
        Relationship(
          id = UUID.randomUUID(),
          from = personUuid,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          product = RelationshipProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now()),
          state = RelationshipState.PENDING,
          createdAt = OffsetDateTime.now()
        )
      val relationshipId   = UUID.randomUUID()

      (() => uuidSupplier.get).expects().returning(institutionUuid).once() // Create institution
      (() => uuidSupplier.get).expects().returning(relationshipId).once()  // Create relationship

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Confirm relationship updated At
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Suspend relationship updated At

      val _ =
        prepareTest(personSeed = personSeed, institutionSeed = institutionSeed, relationshipSeed = relationshipSeed)

      confirmRelationshipWithToken(relationship)

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
      updatedRelationship.state shouldBe RelationshipState.SUSPENDED

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
      val personUuid       = UUID.randomUUID()
      val institutionUuid  = UUID.randomUUID()
      val externalId       = randomString()
      val originId         = randomString()
      val personSeed       = PersonSeed(id = personUuid)
      val institutionSeed  =
        InstitutionSeed(
          externalId = externalId,
          originId = originId,
          description = "Institutions One",
          digitalAddress = "mail1@mail.org",
          address = "address",
          zipCode = "zipCode",
          taxCode = "taxCode",
          products = Option(Map.empty[String, InstitutionProduct]),
          attributes = Seq.empty,
          origin = "IPA",
          institutionType = Option("PA")
        )
      val relationshipSeed =
        RelationshipSeed(
          from = personUuid,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          RelationshipProductSeed(id = "p1", role = "admin")
        )
      val relationship     =
        Relationship(
          id = UUID.randomUUID(),
          from = personUuid,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          product = RelationshipProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now()),
          state = RelationshipState.PENDING,
          createdAt = OffsetDateTime.now()
        )
      val relationshipId   = UUID.randomUUID()

      (() => uuidSupplier.get).expects().returning(institutionUuid).once() // Create institution
      (() => uuidSupplier.get).expects().returning(relationshipId).once()  // Create relationship

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Confirm relationship
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Suspend relationship updated At
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Activate relationship updated At

      val _ =
        prepareTest(personSeed = personSeed, institutionSeed = institutionSeed, relationshipSeed = relationshipSeed)

      confirmRelationshipWithToken(relationship)

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
      updatedRelationship.state shouldBe RelationshipState.ACTIVE

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

  "Enabling relationship" must {
    import RelationshipPartyApiServiceData._

    "succeed" in {
      val personUuid       = UUID.randomUUID()
      val institutionUuid  = UUID.randomUUID()
      val externalId       = randomString()
      val originId         = randomString()
      val personSeed       = PersonSeed(id = personUuid)
      val institutionSeed  =
        InstitutionSeed(
          externalId = externalId,
          originId = originId,
          description = "Institutions One",
          digitalAddress = "mail1@mail.org",
          address = "address",
          zipCode = "zipCode",
          taxCode = "taxCode",
          products = Option(Map.empty[String, InstitutionProduct]),
          attributes = Seq.empty,
          origin = "SELC",
          institutionType = Option("PA")
        )
      val relationshipSeed =
        RelationshipSeed(
          from = personUuid,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          RelationshipProductSeed(id = "p1", role = "admin"),
          state = Option(RelationshipState.TOBEVALIDATED)
        )
      val relationship     =
        Relationship(
          id = UUID.randomUUID(),
          from = personUuid,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          product = RelationshipProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now()),
          state = RelationshipState.TOBEVALIDATED,
          createdAt = OffsetDateTime.now()
        )
      val relationshipId   = UUID.randomUUID()

      (() => uuidSupplier.get).expects().returning(institutionUuid).once() // Create institution
      (() => uuidSupplier.get).expects().returning(relationshipId).once()  // Create relationship

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Confirm relationship
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Enable relationship updated At

      val _ =
        prepareTest(personSeed = personSeed, institutionSeed = institutionSeed, relationshipSeed = relationshipSeed)

      confirmRelationshipWithToken(relationship)

      // Then enable the relationship
      val enableResponse =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships/${relationshipId.toString}/enable",
              method = HttpMethods.POST,
              headers = authorization
            )
          )
          .futureValue

      enableResponse.status shouldBe StatusCodes.NoContent

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
      updatedRelationship.state shouldBe RelationshipState.PENDING

    }

    "fail if relationship does not exist" in {
      val response =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/relationships/non-existing-relationship/enable",
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
      val personUuid       = UUID.randomUUID()
      val institutionUuid  = UUID.randomUUID()
      val externalId       = randomString()
      val originId         = randomString()
      val personSeed       = PersonSeed(id = personUuid)
      val institutionSeed  =
        InstitutionSeed(
          externalId = externalId,
          originId = originId,
          description = "Institutions One",
          digitalAddress = "mail1@mail.org",
          address = "address",
          zipCode = "zipCode",
          taxCode = "taxCode",
          products = Option(Map.empty[String, InstitutionProduct]),
          attributes = Seq.empty,
          origin = "IPA",
          institutionType = Option("PA")
        )
      val relationshipSeed =
        RelationshipSeed(
          from = personUuid,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          RelationshipProductSeed(id = "p1", role = "admin")
        )
      val relationship     =
        Relationship(
          id = UUID.randomUUID(),
          from = personUuid,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          product = RelationshipProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now()),
          state = RelationshipState.PENDING,
          createdAt = OffsetDateTime.now()
        )
      val relationshipId   = UUID.randomUUID()

      (() => uuidSupplier.get).expects().returning(institutionUuid).once() // Create institution
      (() => uuidSupplier.get).expects().returning(relationshipId).once()  // Create relationship

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Confirm relationship
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Delete relationship updated At

      val _ =
        prepareTest(personSeed = personSeed, institutionSeed = institutionSeed, relationshipSeed = relationshipSeed)

      confirmRelationshipWithToken(relationship)

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
      updatedRelationship.state shouldBe RelationshipState.DELETED

    }

    "fail if relationship does not exist" in {
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Delete relationship

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

      (() => uuidSupplier.get).expects().returning(orgId1).once()           // Create institution
      (() => uuidSupplier.get).expects().returning(createTokenUuid0).once() // Create relationship1
      (() => uuidSupplier.get).expects().returning(createTokenUuid1).once() // Create relationship2

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship2

      val relationshipResponse = prepareTest(personSeed1, institutionSeed1, relationshipSeed1, relationshipSeed2)

      val relationships = Unmarshal(relationshipResponse.entity).to[Relationships].futureValue

      val tokenSeed =
        TokenSeed(
          id = tokenId1.toString,
          relationships = relationships,
          "checksum",
          OnboardingContractInfo("1", "test.html")
        )

      val tokenData = Marshal(tokenSeed).to[MessageEntity].map(_.dataBytes).futureValue

      val response = createToken(tokenData)

      response.status shouldBe StatusCodes.Created
    }

    def consumeToken(
      orgId: UUID,
      institutionSeed: InstitutionSeed,
      relationSheepIdManager: UUID,
      relationshipSeedManager: RelationshipSeed,
      relationSheepIdOperator: UUID,
      relationshipSeedOperator: RelationshipSeed,
      token: Token,
      tokenSeed: TokenSeed,
      expectedInstitution: Institution
    ) = {
      (() => uuidSupplier.get).expects().returning(orgId).once()                   // Create institution
      (() => uuidSupplier.get).expects().returning(relationSheepIdManager).once()  // Create relationship1
      (() => uuidSupplier.get).expects().returning(relationSheepIdOperator).once() // Create relationship2

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship2
      (() => offsetDateTimeSupplier.get)
        .expects()
        .returning(timestampValid)
        .repeated(tokenSeed.relationships.items.size)                               // Consume token

      val relationshipResponse =
        prepareTest(personSeed2, institutionSeed, relationshipSeedManager, relationshipSeedOperator)

      Unmarshal(relationshipResponse.entity).to[Relationships].futureValue

      val tokenData = Marshal(tokenSeed).to[MessageEntity].map(_.dataBytes).futureValue

      createToken(tokenData)

      val tokenText = token.id.toString

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

      // check institution updates
      val response =
        Http()
          .singleRequest(
            HttpRequest(uri = s"$url/institutions/${orgId.toString}", method = HttpMethods.GET, headers = authorization)
          )
          .futureValue

      val body = Unmarshal(response.entity).to[Institution].futureValue

      response.status shouldBe StatusCodes.OK

      body shouldBe expectedInstitution
    }

    "consume a token for IPA institution" in {
      consumeToken(
        orgId2,
        institutionSeed2,
        relationshipId1,
        relationshipSeed3,
        relationshipId2,
        relationshipSeed4,
        token1,
        tokenSeed1,
        expected2
      )
    }

    "consume a token for NON IPA institution having products already configured" in {
      consumeToken(
        orgId8,
        institutionSeed8,
        relationshipId15,
        relationshipSeed15,
        relationshipId16,
        relationshipSeed16,
        token8,
        tokenSeed8,
        expected8
      )
    }

    "invalidate a token" in {

      (() => uuidSupplier.get).expects().returning(orgId3).once()          // Create organization
      (() => uuidSupplier.get).expects().returning(relationshipId3).once() // Create relationship1
      (() => uuidSupplier.get).expects().returning(relationshipId4).once() // Create relationship2

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create organization
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship2
      (() => offsetDateTimeSupplier.get)
        .expects()
        .returning(timestampValid)
        .repeated(tokenSeed2.relationships.items.size)                              // Consume token

      val relationshipResponse = prepareTest(personSeed3, institutionSeed3, relationshipSeed5, relationshipSeed6)

      Unmarshal(relationshipResponse.entity).to[Relationships].futureValue

      val tokenData = Marshal(tokenSeed2).to[MessageEntity].map(_.dataBytes).futureValue

      createToken(tokenData)

      val tokenText = token2.id.toString

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

      body.items shouldBe Seq.empty

    }

    "throw an error if the token is expired" in {
      (() => uuidSupplier.get).expects().returning(orgId4).once()          // Create institution
      (() => uuidSupplier.get).expects().returning(relationshipId5).once() // Create relationship1
      (() => uuidSupplier.get).expects().returning(relationshipId6).once() // Create relationship2

      (() => offsetDateTimeSupplier.get).expects().returning(timestampExpired).once() // Create person1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampExpired).once() // Create person2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampExpired).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampExpired).once() // Create relationship1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampExpired).once() // Create relationship2
      (() => offsetDateTimeSupplier.get)
        .expects()
        .returning(timestampExpired)
        .repeated(tokenSeed3.relationships.items.size)                                // Consume token

      val relationshipResponse = prepareTest(personSeed4, institutionSeed4, relationshipSeed7, relationshipSeed8)

      Unmarshal(relationshipResponse.entity).to[Relationships].futureValue

      val tokenData = Marshal(tokenSeed3).to[MessageEntity].map(_.dataBytes).futureValue

      createToken(tokenData)

      val tokenText = token3.id.toString

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
      consumedResponse.status shouldBe StatusCodes.BadRequest
    }

    "throw an error if the token does not exist" in {
      val formData = Multipart.FormData
        .fromFile("doc", MediaTypes.`application/octet-stream`, file = writeToTempFile("hello world"), 100000)
        .toEntity

      val consumedResponse =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/tokens/${UUID.randomUUID().toString}",
              method = HttpMethods.POST,
              headers = multipart,
              entity = formData
            )
          )
          .futureValue
      consumedResponse.status shouldBe StatusCodes.NotFound
    }

    "verify a token" in {

      (() => uuidSupplier.get).expects().returning(orgId5).once()          // Create institution
      (() => uuidSupplier.get).expects().returning(relationshipId7).once() // Create relationship1
      (() => uuidSupplier.get).expects().returning(relationshipId8).once() // Create relationship2

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship2
      (() => offsetDateTimeSupplier.get)
        .expects()
        .returning(timestampValid)
        .repeated(tokenSeed4.relationships.items.size)                              // Consume token

      val relationshipResponse = prepareTest(personSeed5, institutionSeed5, relationshipSeed9, relationshipSeed10)

      Unmarshal(relationshipResponse.entity).to[Relationships].futureValue

      val tokenData = Marshal(tokenSeed4).to[MessageEntity].map(_.dataBytes).futureValue

      createToken(tokenData)

      val tokenText = token4.id.toString

      val response =
        Http()
          .singleRequest(HttpRequest(uri = s"$url/tokens/$tokenText/verify", method = HttpMethods.POST))
          .futureValue

      response.status shouldBe StatusCodes.OK
    }

    "verify a token tobedefined" in {

      (() => uuidSupplier.get).expects().returning(orgId10).once()          // Create institution
      (() => uuidSupplier.get).expects().returning(relationshipId17).once() // Create relationship1
      (() => uuidSupplier.get).expects().returning(relationshipId18).once() // Create relationship2

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship2
      (() => offsetDateTimeSupplier.get)
        .expects()
        .returning(timestampValid)
        .repeated(tokenSeed10.relationships.items.size)                             // Consume token

      val relationshipResponse = prepareTest(personSeed10, institutionSeed10, relationshipSeed19, relationshipSeed20)

      Unmarshal(relationshipResponse.entity).to[Relationships].futureValue

      val tokenData = Marshal(tokenSeed10).to[MessageEntity].map(_.dataBytes).futureValue

      createToken(tokenData)

      val tokenText = token10.id.toString

      val response =
        Http()
          .singleRequest(HttpRequest(uri = s"$url/tokens/$tokenText/verify", method = HttpMethods.POST))
          .futureValue

      response.status shouldBe StatusCodes.OK
    }

    "return 404 trying to verify a non-existent token " in {

      val response =
        Http()
          .singleRequest(
            HttpRequest(uri = s"$url/tokens/${UUID.randomUUID().toString}/verify", method = HttpMethods.POST)
          )
          .futureValue

      response.status shouldBe StatusCodes.NotFound
    }

    "return 409 trying to verify a token already consumed (activated token)" in {
      (() => uuidSupplier.get).expects().returning(orgId6).once()           // Create institution
      (() => uuidSupplier.get).expects().returning(relationshipId9).once()  // Create relationship1
      (() => uuidSupplier.get).expects().returning(relationshipId10).once() // Create relationship2

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship2
      (() => offsetDateTimeSupplier.get)
        .expects()
        .returning(timestampValid)
        .repeated(tokenSeed5.relationships.items.size)                              // Consume token

      val relationshipResponse = prepareTest(personSeed6, institutionSeed6, relationshipSeed11, relationshipSeed12)

      Unmarshal(relationshipResponse.entity).to[Relationships].futureValue

      val tokenData = Marshal(tokenSeed5).to[MessageEntity].map(_.dataBytes).futureValue

      createToken(tokenData)

      val tokenText = token5.id.toString

      val formData = Multipart.FormData
        .fromFile("doc", MediaTypes.`application/octet-stream`, file = writeToTempFile("hello world"), 100000)
        .toEntity

      val _ =
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

      val response =
        Http()
          .singleRequest(HttpRequest(uri = s"$url/tokens/$tokenText/verify", method = HttpMethods.POST))
          .futureValue

      response.status shouldBe StatusCodes.Conflict
    }

    "return 400 trying to verify a token already consumed (rejected token)" in {
      (() => uuidSupplier.get).expects().returning(orgId7).once()           // Create organization
      (() => uuidSupplier.get).expects().returning(relationshipId11).once() // Create relationship1
      (() => uuidSupplier.get).expects().returning(relationshipId12).once() // Create relationship2

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create organization
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship2
      (() => offsetDateTimeSupplier.get)
        .expects()
        .returning(timestampValid)
        .repeated(tokenSeed6.relationships.items.size)                              // Consume token

      val relationshipResponse = prepareTest(personSeed7, institutionSeed7, relationshipSeed13, relationshipSeed14)

      Unmarshal(relationshipResponse.entity).to[Relationships].futureValue

      val tokenData = Marshal(tokenSeed6).to[MessageEntity].map(_.dataBytes).futureValue

      createToken(tokenData)

      val tokenText = token6.id.toString

      val _ =
        Http()
          .singleRequest(HttpRequest(uri = s"$url/tokens/$tokenText", method = HttpMethods.DELETE))
          .futureValue

      val response =
        Http()
          .singleRequest(HttpRequest(uri = s"$url/tokens/$tokenText/verify", method = HttpMethods.POST))
          .futureValue

      response.status shouldBe StatusCodes.BadRequest
    }

    "update a token" in {

      (() => uuidSupplier.get).expects().returning(orgId9).once()           // Create organization
      (() => uuidSupplier.get).expects().returning(relationshipId17).once() // Create relationship1
      (() => uuidSupplier.get).expects().returning(relationshipId18).once() // Create relationship2

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person2
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create organization
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship1
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship2

      val relationshipResponse = prepareTest(personSeed8, institutionSeed9, relationshipSeed17, relationshipSeed18)

      Unmarshal(relationshipResponse.entity).to[Relationships].futureValue

      val tokenData = Marshal(tokenSeed9).to[MessageEntity].map(_.dataBytes).futureValue

      createToken(tokenData)

      val tokenText      = token9.id.toString
      val digest         = DigestSeed(UUID.randomUUID().toString)
      val data           = Marshal(digest).to[MessageEntity].map(_.dataBytes).futureValue
      val updateResponse =
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url/tokens/$tokenText/digest",
              method = HttpMethods.POST,
              headers = authorization,
              entity = HttpEntity(ContentTypes.`application/json`, data)
            )
          )
          .futureValue

      updateResponse.status shouldBe StatusCodes.OK

      val body = Unmarshal(updateResponse.entity).to[TokenText].futureValue

      body.token shouldBe tokenText

    }
  }

  "Lookup a relationship by UUID" must {
    "return 400 when the input parameter is not a valid UUID" in {
      // given a random UUID
      val uuid = "YADA-YADA"

      // when looking up for the corresponding institution
      val response =
        Http()
          .singleRequest(
            HttpRequest(uri = s"$url/relationships/$uuid", method = HttpMethods.GET, headers = authorization)
          )
          .futureValue

      // then
      response.status shouldBe StatusCodes.BadRequest
    }

    "return 404 when the relationship does not exist" in {
      // given a random UUID

      val uuid = UUID.randomUUID().toString

      // when looking up for the corresponding institution
      val response =
        Http()
          .singleRequest(
            HttpRequest(uri = s"$url/relationships/$uuid", method = HttpMethods.GET, headers = authorization)
          )
          .futureValue

      // then
      response.status shouldBe StatusCodes.NotFound
    }

    "return the institution payload when it exists" in {
      import RelationshipPartyApiServiceData._

      // given

      val personUuid      = UUID.randomUUID()
      val institutionUuid = UUID.randomUUID()
      val relationshipId  = UUID.randomUUID()
      val externalId      = randomString()
      val originId        = randomString()

      val personSeed      = PersonSeed(personUuid)
      val institutionSeed =
        InstitutionSeed(
          externalId = externalId,
          originId = originId,
          description = "Institutions One",
          digitalAddress = "mail1@mail.org",
          address = "address",
          zipCode = "zipCode",
          taxCode = "taxCode",
          products = Option(Map.empty[String, InstitutionProduct]),
          attributes = Seq.empty,
          origin = "IPA",
          institutionType = Option("PA")
        )
      val rlSeed          =
        RelationshipSeed(
          from = personUuid,
          to = institutionUuid,
          role = PartyRole.MANAGER,
          RelationshipProductSeed(id = "p1", role = "admin")
        )

      (() => uuidSupplier.get).expects().returning(institutionUuid).once() // Create institution
      (() => uuidSupplier.get).expects().returning(relationshipId).once()  // Create relationship

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship

      prepareTest(personSeed = personSeed, institutionSeed = institutionSeed, relationshipSeed = rlSeed)

      // when
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

      // then
      response.status shouldBe StatusCodes.OK
      val body = Unmarshal(response.entity).to[Relationship].futureValue
      body shouldBe
        Relationship(
          id = relationshipId,
          from = personUuid,
          to = institutionUuid,
          role = rlSeed.role,
          product = RelationshipProduct(id = "p1", role = "admin", createdAt = timestampValid),
          state = RelationshipState.PENDING,
          filePath = None,
          fileName = None,
          contentType = None,
          createdAt = timestampValid,
          updatedAt = None
        )
    }
  }
}
