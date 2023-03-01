package it.pagopa.interop.partymanagement

import akka.actor
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.cluster.typed.{Cluster, Join}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.{AuthenticationDirective, SecurityDirectives}
import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.utils.AkkaUtils
import it.pagopa.interop.commons.utils.AkkaUtils.Authenticator
import it.pagopa.interop.partymanagement.api.impl.PartyApiMarshallerImpl.sprayJsonMarshaller
import it.pagopa.interop.partymanagement.api.impl.{
  ExternalApiMarshallerImpl,
  ExternalApiServiceImpl,
  NewDesignExposureApiMarshallerImpl,
  NewDesignExposureApiServiceImpl,
  PartyApiMarshallerImpl,
  PartyApiServiceImpl,
  PublicApiMarshallerImpl,
  PublicApiServiceImpl,
  institutionSeedFormat,
  personSeedFormat,
  relationshipSeedFormat
}
import it.pagopa.interop.partymanagement.api._
import it.pagopa.interop.partymanagement.model._
import it.pagopa.interop.partymanagement.model.persistence.PartyPersistentBehavior
import it.pagopa.interop.partymanagement.server.Controller
import it.pagopa.interop.partymanagement.server.impl.Main.behaviorFactory
import it.pagopa.interop.partymanagement.service.{InstitutionService, RelationshipService}
import it.pagopa.interop.partymanagement.service.impl.{InstitutionServiceImpl, RelationshipServiceImpl}
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object RelationshipServiceSpec {
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
}

class RelationshipServiceSpec extends ScalaTestWithActorTestKit(RelationshipServiceSpec.config) with AnyWordSpecLike {

  var relationshipService: RelationshipService = _

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

    relationshipService = new RelationshipServiceImpl(system, sharding, persistentEntity)
    val institutionService: InstitutionService = new InstitutionServiceImpl(system, sharding, persistentEntity)

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

    val newDesignExposureApiService: NewDesignExposureApiService =
      new NewDesignExposureApiServiceImpl(
        system = system,
        sharding = sharding,
        entity = persistentEntity,
        relationshipService,
        institutionService
      )

    val newDesignExposureApi: NewDesignExposureApi =
      new NewDesignExposureApi(newDesignExposureApiService, NewDesignExposureApiMarshallerImpl, wrappingDirective)

    val healthApi: HealthApi = mock[HealthApi]

    controller = Some(
      new Controller(
        health = healthApi,
        party = partyApi,
        external = externalApi,
        public = publicApi,
        newDesignExposure = newDesignExposureApi
      )(classicSystem)
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

  "Lookup a relationship by UUID" must {

    "return empty Option when the relationship does not exist" in {
      // given a random UUID

      val uuid = UUID.randomUUID()

      // when looking up for the corresponding institution
      val result = relationshipService.getRelationshipById(uuid).futureValue

      // then
      result shouldBe None
    }

    "return the relationship when it exists" in {
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
      val result = relationshipService.getRelationshipById(relationshipId).futureValue

      // then
      result.get shouldBe
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
