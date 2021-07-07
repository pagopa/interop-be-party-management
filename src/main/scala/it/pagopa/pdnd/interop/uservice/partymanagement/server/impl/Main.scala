package it.pagopa.pdnd.interop.uservice.partymanagement.server.impl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, ShardedDaemonProcess}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.cluster.typed.{Cluster, Subscribe}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.persistence.typed.PersistenceId
import akka.projection.ProjectionBehavior
import akka.{actor => classic}
import it.pagopa.pdnd.interop.uservice.partymanagement.api.impl._
import it.pagopa.pdnd.interop.uservice.partymanagement.api.{HealthApi, PartyApi}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.{Authenticator, CorsSupport}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.Problem
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.{
  Command,
  PartyPersistentBehavior,
  PartyPersistentProjection
}
import it.pagopa.pdnd.interop.uservice.partymanagement.server.Controller
import it.pagopa.pdnd.interop.uservice.partymanagement.service.UUIDSupplier
import it.pagopa.pdnd.interop.uservice.partymanagement.service.impl.UUIDSupplierImpl
import kamon.Kamon
import spray.json.{JsValue, enrichAny}

import scala.jdk.CollectionConverters._

@SuppressWarnings(
  Array("org.wartremover.warts.StringPlusAny", "org.wartremover.warts.Nothing", "org.wartremover.warts.Throw")
)
object Main extends App with CorsSupport {

  Kamon.init()

  def buildPersistentEntity(): Entity[Command, ShardingEnvelope[Command]] =
    Entity(typeKey = PartyPersistentBehavior.TypeKey) { entityContext =>
      PartyPersistentBehavior(
        entityContext.shard,
        PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)
      )
    }

  locally {
    val _ = ActorSystem[Nothing](
      Behaviors.setup[Nothing] { context =>
        import akka.actor.typed.scaladsl.adapter._
        implicit val classicSystem: classic.ActorSystem = context.system.toClassic
        val cluster: Cluster                            = Cluster(context.system)

        context.log.error("Started [" + context.system + "], cluster.selfAddress = " + cluster.selfMember.address + ")")

        val sharding: ClusterSharding = ClusterSharding(context.system)

        val partyPersistentEntity: Entity[Command, ShardingEnvelope[Command]] = buildPersistentEntity()

        val _ = sharding.init(partyPersistentEntity)

        val settings: ClusterShardingSettings = partyPersistentEntity.settings match {
          case None    => ClusterShardingSettings(context.system)
          case Some(s) => s
        }

        val persistence =
          classicSystem.classicSystem.settings.config.getString("uservice-party-management.persistence")

        if (persistence == "cassandra") {
          val partyPersistentProjection = new PartyPersistentProjection(context.system, partyPersistentEntity)

          ShardedDaemonProcess(context.system).init[ProjectionBehavior.Command](
            name = "party-projections",
            numberOfInstances = settings.numberOfShards,
            behaviorFactory = (i: Int) => ProjectionBehavior(partyPersistentProjection.projections(i)),
            stopMessage = ProjectionBehavior.Stop
          )
        }

        val uuidSupplier: UUIDSupplier = new UUIDSupplierImpl

        val partyApi: PartyApi = new PartyApi(
          new PartyApiServiceImpl(context.system, sharding, partyPersistentEntity, uuidSupplier),
          new PartyApiMarshallerImpl(),
          SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
        )

        val healthApi: HealthApi = new HealthApi(
          new HealthServiceApiImpl(),
          new HealthApiMarshallerImpl(),
          SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
        )

        val _ = AkkaManagement.get(classicSystem).start()

        val controller = new Controller(
          healthApi,
          partyApi,
          validationExceptionToRoute = Some(e => {
            val results = e.results()
            results.crumbs().asScala.foreach { crumb =>
              println(crumb.crumb())
            }
            results.items().asScala.foreach { item =>
              println(item.dataCrumbs())
              println(item.dataJsonPointer())
              println(item.schemaCrumbs())
              println(item.message())
              println(item.severity())
            }
            val message: JsValue =
              Problem(Some(e.results().items().asScala.map(_.message()).mkString("\n")), 400, "some error").toJson
            complete((400, message))
          })
        )

        val _ = Http().newServerAt("0.0.0.0", 8088).bind(corsHandler(controller.routes))

        val listener = context.spawn(
          Behaviors.receive[ClusterEvent.MemberEvent]((ctx, event) => {
            ctx.log.error("MemberEvent: {}", event)
            Behaviors.same
          }),
          "listener"
        )

        cluster.subscriptions ! Subscribe(listener, classOf[ClusterEvent.MemberEvent])

        val _ = AkkaManagement(classicSystem).start()
        ClusterBootstrap.get(classicSystem).start()
        Behaviors.empty
      },
      "pdnd-interop-uservice-party-management"
    )

  }
}
