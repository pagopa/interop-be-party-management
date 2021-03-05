package it.pagopa.pdnd.interop.uservice.partymanagement.server.impl

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.management.scaladsl.AkkaManagement
import it.pagopa.pdnd.interop.uservice.partymanagement.api.impl.{
  HealthApiMarshallerImpl,
  HealthServiceApiImpl,
  PartyApiMarshallerImpl,
  PartyApiServiceImpl
}
import it.pagopa.pdnd.interop.uservice.partymanagement.api.{HealthApi, PartyApi}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.{Authenticator, classicActorSystem}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyPersistentBehavior
import it.pagopa.pdnd.interop.uservice.partymanagement.server.Controller
import kamon.Kamon

import scala.concurrent.Future

object Main extends App {

  Kamon.init()

  val partyCommander = ActorSystem(PartyPersistentBehavior(), "pdnd-interop-uservice-party-management")

  val partyApi: PartyApi = new PartyApi(
    new PartyApiServiceImpl(partyCommander),
    new PartyApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  val healthApi: HealthApi = new HealthApi(
    new HealthServiceApiImpl(),
    new HealthApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  locally {
    val _ = AkkaManagement.get(classicActorSystem).start()
  }

  val controller: Controller = new Controller(healthApi, partyApi)

  val bindingFuture: Future[Http.ServerBinding] =
    Http().newServerAt("0.0.0.0", 8088).bind(controller.routes)

}
