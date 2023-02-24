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
import it.pagopa.interop.partymanagement.api._
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
  institutionSeedFormat
}
import it.pagopa.interop.partymanagement.model.party.Party
import it.pagopa.interop.partymanagement.model.persistence.PartyPersistentBehavior
import it.pagopa.interop.partymanagement.server.Controller
import it.pagopa.interop.partymanagement.server.impl.Main.behaviorFactory
import it.pagopa.interop.partymanagement.service.impl.{InstitutionServiceImpl, RelationshipServiceImpl}
import it.pagopa.interop.partymanagement.service.{InstitutionService, RelationshipService}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object InstitutionServiceSpec {
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

class InstitutionServiceSpec extends ScalaTestWithActorTestKit(InstitutionServiceSpec.config) with AnyWordSpecLike {

  var institutionService: InstitutionService = _

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
    institutionService = new InstitutionServiceImpl(system, sharding, persistentEntity)

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
        relationshipService
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

  "Working on institutions" must {
    import InstitutionsPartyApiServiceData._

    "return the institution if exists" in {

      (() => uuidSupplier.get).expects().returning(institutionUuid3).once()

      (() => offsetDateTimeSupplier.get).expects().returning(timestampValid).once()

      prepareTest(institutionSeed3)

      val response = institutionService.getInstitutionById(institutionUuid3).futureValue

      Party.convertToApi(response.get).left.getOrElse(null) shouldBe expected3
    }
  }
}
