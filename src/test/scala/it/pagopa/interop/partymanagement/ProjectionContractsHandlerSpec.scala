package it.pagopa.interop.partymanagement

import akka.actor
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.cluster.typed.{Cluster, Join}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.{AuthenticationDirective, SecurityDirectives}
import akka.projection.eventsourced.EventEnvelope
import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.queue.kafka.KafkaPublisher
import it.pagopa.interop.commons.utils.AkkaUtils
import it.pagopa.interop.commons.utils.AkkaUtils.Authenticator
import it.pagopa.interop.partymanagement.api._
import it.pagopa.interop.partymanagement.api.impl.PartyApiMarshallerImpl.sprayJsonMarshaller
import it.pagopa.interop.partymanagement.api.impl.{
  ExternalApiMarshallerImpl,
  ExternalApiServiceImpl,
  PartyApiMarshallerImpl,
  PartyApiServiceImpl,
  PublicApiMarshallerImpl,
  PublicApiServiceImpl,
  institutionSeedFormat,
  personSeedFormat,
  relationshipSeedFormat
}
import it.pagopa.interop.partymanagement.model._
import it.pagopa.interop.partymanagement.model.party.{
  InstitutionOnboarded,
  InstitutionOnboardedBilling,
  InstitutionOnboardedNotification,
  Token
}
import it.pagopa.interop.partymanagement.model.persistence.{
  PartyPersistentBehavior,
  PartyRelationshipConfirmed,
  ProjectionContractsHandler,
  TokenAdded
}
import it.pagopa.interop.partymanagement.server.Controller
import it.pagopa.interop.partymanagement.server.impl.Main.behaviorFactory
import it.pagopa.interop.partymanagement.service.impl.{InstitutionServiceImpl, RelationshipServiceImpl}
import it.pagopa.interop.partymanagement.service.{InstitutionService, RelationshipService}
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpecLike
import spray.json.JsonWriter

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object ProjectionContractsHandlerSpec {
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

class ProjectionContractsHandlerSpec
    extends ScalaTestWithActorTestKit(ProjectionContractsHandlerSpec.config)
    with MockFactory
    with AnyWordSpecLike {

  var datalakeContractsPublisherMock: KafkaPublisher = _
  var projectionHandler: ProjectionContractsHandler  = _

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

    datalakeContractsPublisherMock = mock[KafkaPublisher]
    projectionHandler =
      new ProjectionContractsHandler(system, sharding, persistentEntity, relationshipService, institutionService)(
        "tag",
        datalakeContractsPublisherMock
      )(system.executionContext)

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

  "Projecting PartyRelationshipConfirmed event" must {
    import RelationshipPartyApiServiceData._

    def storeRelationship(
      institutionUuid: UUID,
      externalId: String,
      originId: String,
      relationshipId: UUID,
      role: PartyRole
    ) = {
      val personUuid = UUID.randomUUID()

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
          products = Option(
            Map(
              "p1" -> InstitutionProduct(
                "p1",
                Option("pricingPlan"),
                Billing("VATNUMBER", "RECIPIENTCODE", Option(true))
              )
            )
          ),
          attributes = Seq.empty,
          origin = "IPA",
          institutionType = Option("PA")
        )
      val rlSeed          =
        RelationshipSeed(
          from = personUuid,
          to = institutionUuid,
          role = role,
          RelationshipProductSeed(id = "p1", role = "admin")
        )

      (() => uuidSupplier.get).expects().returning(institutionUuid).once() // Create institution
      (() => uuidSupplier.get).expects().returning(relationshipId).once()  // Create relationship

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create person
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create institution
      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once() // Create relationship

      prepareTest(personSeed = personSeed, institutionSeed = institutionSeed, relationshipSeed = rlSeed)
    }

    "publish on kafka if manager" in {
      val managerConfirmEvent: PartyRelationshipConfirmed = PartyRelationshipConfirmed(
        UUID.randomUUID(),
        "filePath",
        "fileName",
        "contentType",
        UUID.randomUUID(),
        OffsetDateTime.now(ZoneOffset.UTC)
      )
      val institutionUuid                                 = UUID.randomUUID()
      val externalId                                      = randomString()
      val originId                                        = randomString()

      val expectedPayload: InstitutionOnboardedNotification = InstitutionOnboardedNotification(
        id = Option(managerConfirmEvent.onboardingTokenId),
        internalIstitutionID = institutionUuid,
        product = "p1",
        state = "ACTIVE",
        filePath = Option("filePath"),
        fileName = Option("fileName"),
        contentType = Option("contentType"),
        onboardingTokenId = Option(managerConfirmEvent.onboardingTokenId),
        pricingPlan = Option("pricingPlan"),
        institution = InstitutionOnboarded(
          institutionType = "PA",
          description = "Institutions One",
          digitalAddress = Option("mail1@mail.org"),
          address = Option("address"),
          taxCode = "taxCode",
          origin = "IPA",
          originId = originId
        ),
        billing = Option(InstitutionOnboardedBilling("VATNUMBER", "RECIPIENTCODE", Option(true))),
        updatedAt = Option(managerConfirmEvent.timestamp),
        createdAt = OffsetDateTime.now(),
        closedAt = Option.empty,
        psp = Option.empty
      )

      storeRelationship(
        institutionUuid,
        externalId,
        originId,
        managerConfirmEvent.partyRelationshipId,
        PartyRole.MANAGER
      )

      (datalakeContractsPublisherMock
        .send(_: InstitutionOnboardedNotification)(_: JsonWriter[InstitutionOnboardedNotification]))
        .expects(expectedPayload, *)
        .returning(Future.successful("OK"))
        .never()

      projectionHandler.process(new EventEnvelope(null, null, 0, managerConfirmEvent, 0))
    }

    "not publish on kafka if not manager" in {
      val notManagerEvent: PartyRelationshipConfirmed = PartyRelationshipConfirmed(
        UUID.randomUUID(),
        "filePath",
        "fileName",
        "contentType",
        UUID.randomUUID(),
        OffsetDateTime.now
      )
      val institutionUuid                             = UUID.randomUUID()
      val externalId                                  = randomString()
      val originId                                    = randomString()

      storeRelationship(institutionUuid, externalId, originId, notManagerEvent.partyRelationshipId, PartyRole.DELEGATE)

      (datalakeContractsPublisherMock
        .send(_: InstitutionOnboardedNotification)(_: JsonWriter[InstitutionOnboardedNotification]))
        .expects(*, *)
        .never()

      projectionHandler.process(new EventEnvelope(null, null, 0, notManagerEvent, 0))
    }
  }

  "Projecting other events" must {
    "not publish on kafka" in {
      val event: TokenAdded = TokenAdded(new Token(UUID.randomUUID(), "checksum", Seq.empty, null, null))

      (datalakeContractsPublisherMock
        .send(_: Any)(_: JsonWriter[Any]))
        .expects(*, *)
        .never()

      projectionHandler.process(new EventEnvelope(null, null, 0, event, 0))

    }
  }
}
