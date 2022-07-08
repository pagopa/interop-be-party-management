package it.pagopa.interop.partymanagement.server.impl

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.ClusterEvent
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext, ShardedDaemonProcess}
import akka.cluster.typed.{Cluster, Subscribe}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.persistence.typed.PersistenceId
import akka.projection.ProjectionBehavior
import akka.{actor => classic}
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.interop.commons.files.StorageConfiguration
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.jwt.service.JWTReader
import it.pagopa.interop.commons.jwt.service.impl.{DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.jwt.{JWTConfiguration, PublicKeysHolder}
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.ValidationRequestError
import it.pagopa.interop.commons.utils.service.UUIDSupplier
import it.pagopa.interop.commons.utils.service.impl.UUIDSupplierImpl
import it.pagopa.interop.commons.utils.{AkkaUtils, OpenapiUtils}
import it.pagopa.interop.partymanagement.api.impl.{
  ExternalApiMarshallerImpl,
  ExternalApiServiceImpl,
  HealthApiMarshallerImpl,
  HealthServiceApiImpl,
  PartyApiMarshallerImpl,
  PartyApiServiceImpl,
  PublicApiMarshallerImpl,
  PublicApiServiceImpl,
  problemOf
}
import it.pagopa.interop.partymanagement.api.{ExternalApi, HealthApi, PartyApi, PublicApi}
import it.pagopa.interop.partymanagement.common.system.ApplicationConfiguration
import it.pagopa.interop.partymanagement.common.system.ApplicationConfiguration.{
  kafkaBootstrapServers,
  kafkaDatalakeContractsSaslJaasConfig,
  kafkaDatalakeContractsTopic,
  kafkaSaslMechanism,
  kafkaSecurityProtocol,
  numberOfProjectionTags,
  projectionTag,
  projectionsEnabled
}
import it.pagopa.interop.partymanagement.model.persistence.{
  Command,
  PartyPersistentBehavior,
  PartyPersistentContractsProjection
}
import it.pagopa.interop.partymanagement.server.Controller
import it.pagopa.interop.partymanagement.service.OffsetDateTimeSupplier
import it.pagopa.interop.partymanagement.service.impl.OffsetDateTimeSupplierImp
import kamon.Kamon
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext
import scala.util.Try
import buildinfo.BuildInfo
import it.pagopa.interop.commons.queue.kafka.KafkaPublisher
import it.pagopa.interop.commons.queue.kafka.impl.KafkaPublisherImpl

object Main extends App {

  System.setProperty("kanela.show-banner", "false")

  val dependenciesLoaded: Try[(FileManager, JWTReader)] = for {
    fileManager <- FileManager.getConcreteImplementation(StorageConfiguration.runtimeFileManager)
    keyset      <- JWTConfiguration.jwtReader.loadKeyset()
    jwtValidator = new DefaultJWTReader with PublicKeysHolder {
      var publicKeyset                                                                 = keyset
      override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
        getClaimsVerifier(audience = ApplicationConfiguration.jwtAudience)
    }
  } yield (fileManager, jwtValidator)

  val (fileManager, jwtValidator) =
    dependenciesLoaded.get // THIS IS THE END OF THE WORLD. Exceptions are welcomed here.

  Kamon.init()

  def behaviorFactory(offsetDateTimeSupplier: OffsetDateTimeSupplier): EntityContext[Command] => Behavior[Command] = {
    entityContext =>
      val index = math.abs(entityContext.entityId.hashCode % numberOfProjectionTags)
      PartyPersistentBehavior(
        entityContext.shard,
        PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId),
        offsetDateTimeSupplier,
        projectionTag(index)
      )
  }

  val actorSystem = ActorSystem[Nothing](
    Behaviors.setup[Nothing] { context =>
      import akka.actor.typed.scaladsl.adapter._
      implicit val classicSystem: classic.ActorSystem = context.system.toClassic
      implicit val executionContext: ExecutionContext = context.system.executionContext

      val cluster = Cluster(context.system)

      context.log.info(s"""Started [ ${context.system} ]
                          |   cluster.selfAddress   = ${cluster.selfMember.address}
                          |   file manager type     = ${fileManager.getClass.getName}
                          |   build info            = ${buildinfo.BuildInfo.toString}""".stripMargin)

      val uuidSupplier: UUIDSupplier                     = new UUIDSupplierImpl
      val offsetDateTimeSupplier: OffsetDateTimeSupplier = OffsetDateTimeSupplierImp

      val sharding: ClusterSharding = ClusterSharding(context.system)

      val partyPersistentEntity: Entity[Command, ShardingEnvelope[Command]] =
        Entity(PartyPersistentBehavior.TypeKey)(behaviorFactory(offsetDateTimeSupplier))

      val _ = sharding.init(partyPersistentEntity)

      if (projectionsEnabled) {
        val dbConfig: DatabaseConfig[JdbcProfile] =
          DatabaseConfig.forConfig("akka-persistence-jdbc.shared-databases.slick")

        val datalakeContractsPublisher: KafkaPublisher = new KafkaPublisherImpl(
          context.system,
          kafkaDatalakeContractsTopic,
          kafkaBootstrapServers,
          Map(
            "security.protocol" -> kafkaSecurityProtocol,
            "sasl.jaas.config"  -> kafkaDatalakeContractsSaslJaasConfig,
            "sasl.mechanism"    -> kafkaSaslMechanism
          )
        )

        val partyPersistentContractsProjection =
          new PartyPersistentContractsProjection(context.system, dbConfig, datalakeContractsPublisher)

        ShardedDaemonProcess(context.system).init[ProjectionBehavior.Command](
          name = "party-contracts-projections",
          numberOfInstances = numberOfProjectionTags,
          behaviorFactory = (i: Int) => ProjectionBehavior(partyPersistentContractsProjection.projection(projectionTag(i))),
          stopMessage = ProjectionBehavior.Stop
        )
      }

      val partyApi: PartyApi = new PartyApi(
        new PartyApiServiceImpl(
          system = context.system,
          sharding = sharding,
          entity = partyPersistentEntity,
          uuidSupplier = uuidSupplier,
          offsetDateTimeSupplier = offsetDateTimeSupplier
        ),
        PartyApiMarshallerImpl,
        jwtValidator.OAuth2JWTValidatorAsContexts
      )

      val externalApi: ExternalApi = new ExternalApi(
        new ExternalApiServiceImpl(system = context.system, sharding = sharding, entity = partyPersistentEntity),
        ExternalApiMarshallerImpl,
        jwtValidator.OAuth2JWTValidatorAsContexts
      )

      val publicApi: PublicApi = new PublicApi(
        new PublicApiServiceImpl(
          system = context.system,
          sharding = sharding,
          entity = partyPersistentEntity,
          fileManager = fileManager
        ),
        PublicApiMarshallerImpl,
        SecurityDirectives.authenticateBasic("Public", AkkaUtils.PassThroughAuthenticator)
      )

      val healthApi: HealthApi =
        new HealthApi(new HealthServiceApiImpl(), HealthApiMarshallerImpl, jwtValidator.OAuth2JWTValidatorAsContexts)

      val _ = AkkaManagement.get(classicSystem).start()

      val controller = new Controller(
        health = healthApi,
        party = partyApi,
        external = externalApi,
        public = publicApi,
        validationExceptionToRoute = Some(report => {
          val error =
            problemOf(
              StatusCodes.BadRequest,
              ValidationRequestError(OpenapiUtils.errorFromRequestValidationReport(report))
            )
          complete(error.status, error)(HealthApiMarshallerImpl.toEntityMarshallerProblem)
        })
      )

      val _ = Http().newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(controller.routes)

      val listener = context.spawn(
        Behaviors.receive[ClusterEvent.MemberEvent]((ctx, event) => {
          ctx.log.info("MemberEvent: {}", event)
          Behaviors.same
        }),
        "listener"
      )

      Cluster(context.system).subscriptions ! Subscribe(listener, classOf[ClusterEvent.MemberEvent])

      val _ = AkkaManagement(classicSystem).start()
      ClusterBootstrap.get(classicSystem).start()
      Behaviors.empty
    },
    BuildInfo.name
  )

  actorSystem.whenTerminated.onComplete { case _ => Kamon.stop() }(scala.concurrent.ExecutionContext.global)

}
