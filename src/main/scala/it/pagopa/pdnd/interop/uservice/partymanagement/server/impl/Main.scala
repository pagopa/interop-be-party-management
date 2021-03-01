package it.pagopa.pdnd.interop.uservice.partymanagement.server.impl

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.management.scaladsl.AkkaManagement
import it.pagopa.pdnd.interop.uservice.partymanagement.api.impl.{
  PartyApiMarshallerImpl,
  PartyApiServiceImpl,
  HealthApiMarshallerImpl,
  HealthServiceApiImpl
}
import it.pagopa.pdnd.interop.uservice.partymanagement.api.{HealthApi, PartyApi}
import it.pagopa.pdnd.interop.uservice.partymanagement.server.Controller
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.{Authenticator, classicActorSystem}
import kamon.Kamon

object Main extends App {

  Kamon.init()

  val partyApi: PartyApi = new PartyApi(
    new PartyApiServiceImpl(),
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

  val controller = new Controller(healthApi, partyApi)

  val bindingFuture = Http().newServerAt("0.0.0.0", 8088).bind(controller.routes)
}
