package it.pagopa.pdnd.interop.uservice.partymanagement.server.impl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, ShardedDaemonProcess}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.cluster.typed.{Cluster, Subscribe}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.persistence.typed.PersistenceId
import akka.projection.ProjectionBehavior
import akka.{actor => classic}
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.pdnd.interop.commons.files.StorageConfiguration
import it.pagopa.pdnd.interop.commons.files.service.FileManager
import it.pagopa.pdnd.interop.commons.jwt.service.JWTReader
import it.pagopa.pdnd.interop.commons.jwt.service.impl.{DefaultJWTReader, getClaimsVerifier}
import it.pagopa.pdnd.interop.commons.jwt.{JWTConfiguration, PublicKeysHolder}
import it.pagopa.pdnd.interop.commons.utils.OpenapiUtils
import it.pagopa.pdnd.interop.commons.utils.errors.GenericComponentErrors.ValidationRequestError
import it.pagopa.pdnd.interop.commons.utils.service.UUIDSupplier
import it.pagopa.pdnd.interop.commons.utils.service.impl.UUIDSupplierImpl
import it.pagopa.pdnd.interop.uservice.partymanagement.api.impl.{
  HealthApiMarshallerImpl,
  HealthServiceApiImpl,
  PartyApiMarshallerImpl,
  PartyApiServiceImpl,
  problemOf
}
import it.pagopa.pdnd.interop.uservice.partymanagement.api.{HealthApi, PartyApi}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.ApplicationConfiguration
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.{
  Command,
  PartyPersistentBehavior,
  PartyPersistentProjection
}
import it.pagopa.pdnd.interop.uservice.partymanagement.server.Controller
import it.pagopa.pdnd.interop.uservice.partymanagement.service.OffsetDateTimeSupplier
import it.pagopa.pdnd.interop.uservice.partymanagement.service.impl.OffsetDateTimeSupplierImp
import kamon.Kamon
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext
import scala.util.Try

object Main extends App {

  val dependenciesLoaded: Try[(FileManager, JWTReader)] = for {
    fileManager <- FileManager.getConcreteImplementation(StorageConfiguration.runtimeFileManager)
    keyset      <- JWTConfiguration.jwtReader.loadKeyset()
    jwtValidator = new DefaultJWTReader with PublicKeysHolder {
      var publicKeyset                                                                 = keyset
      override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] = getClaimsVerifier()
    }
  } yield (fileManager, jwtValidator)

  val (fileManager, jwtValidator) =
    dependenciesLoaded.get //THIS IS THE END OF THE WORLD. Exceptions are welcomed here.

  def buildPersistentEntity(
    offsetDateTimeSupplier: OffsetDateTimeSupplier
  ): Entity[Command, ShardingEnvelope[Command]] =
    Entity(typeKey = PartyPersistentBehavior.TypeKey) { entityContext =>
      PartyPersistentBehavior(
        entityContext.shard,
        PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId),
        offsetDateTimeSupplier
      )
    }

  Kamon.init()

  locally {
    val _ = ActorSystem[Nothing](
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
          buildPersistentEntity(offsetDateTimeSupplier)

        val _ = sharding.init(partyPersistentEntity)

        val settings: ClusterShardingSettings = partyPersistentEntity.settings match {
          case None    => ClusterShardingSettings(context.system)
          case Some(s) => s
        }

        val persistence: String =
          classicSystem.classicSystem.settings.config.getString("akka.persistence.journal.plugin")

        if (persistence == "jdbc-journal") {
          val dbConfig: DatabaseConfig[JdbcProfile] =
            DatabaseConfig.forConfig("akka-persistence-jdbc.shared-databases.slick")
          val partyPersistentProjection = PartyPersistentProjection(context.system, partyPersistentEntity, dbConfig)

          ShardedDaemonProcess(context.system).init[ProjectionBehavior.Command](
            name = "party-projections",
            numberOfInstances = settings.numberOfShards,
            behaviorFactory = (i: Int) => ProjectionBehavior(partyPersistentProjection.projections(i)),
            stopMessage = ProjectionBehavior.Stop
          )
        }

        val partyApi: PartyApi = new PartyApi(
          new PartyApiServiceImpl(
            context.system,
            sharding,
            partyPersistentEntity,
            uuidSupplier,
            offsetDateTimeSupplier,
            fileManager
          ),
          PartyApiMarshallerImpl,
          jwtValidator.OAuth2JWTValidatorAsContexts
        )

        val healthApi: HealthApi =
          new HealthApi(new HealthServiceApiImpl(), HealthApiMarshallerImpl, jwtValidator.OAuth2JWTValidatorAsContexts)

        val _ = AkkaManagement.get(classicSystem).start()

        val controller = new Controller(
          healthApi,
          partyApi,
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
      "pdnd-interop-uservice-party-management"
    )

  }
}
