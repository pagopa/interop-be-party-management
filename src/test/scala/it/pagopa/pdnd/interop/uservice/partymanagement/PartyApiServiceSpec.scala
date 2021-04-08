package it.pagopa.pdnd.interop.uservice.partymanagement

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.SecurityDirectives
import it.pagopa.pdnd.interop.uservice.partymanagement.api.impl.{PartyApiMarshallerImpl, PartyApiServiceImpl}
import it.pagopa.pdnd.interop.uservice.partymanagement.api.{HealthApi, PartyApi, PartyApiMarshaller, PartyApiService}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.{
  Authenticator,
  classicActorSystem,
  executionContext
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyPersistentBehavior
import it.pagopa.pdnd.interop.uservice.partymanagement.server.Controller
import org.scalatest.{BeforeAndAfterAll, Suites}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PartyApiServiceSpec
    extends Suites(
      OrganizationsPartyApiServiceSpec(uuidSupplier),
      PersonPartyApiServiceSpec(uuidSupplier),
      RelationshipPartyApiServiceSpec(uuidSupplier)
    )
    with BeforeAndAfterAll {

  val partyApiMarshaller: PartyApiMarshaller = new PartyApiMarshallerImpl

  var controller: Option[Controller]                 = None
  var bindServer: Option[Future[Http.ServerBinding]] = None

  override def beforeAll(): Unit = {

    val wrappingDirective: Directive1[Unit] = SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
    val commander                           = ActorSystem(PartyPersistentBehavior(), "pdnd-interop-uservice-party-management")
    val partyApiService: PartyApiService    = new PartyApiServiceImpl(commander, uuidSupplier)

    val partyApi: PartyApi =
      new PartyApi(partyApiService, partyApiMarshaller, wrappingDirective)

    val healthApi: HealthApi = mock[HealthApi]

    controller = Some(new Controller(healthApi, partyApi))

    controller foreach { controller =>
      bindServer = Some(
        Http()
          .newServerAt("0.0.0.0", 18088)
          .bind(controller.routes)
      )

      Await.result(bindServer.get, 100.seconds)
    }

  }

  override def afterAll(): Unit = {

    bindServer.foreach(_.foreach(_.unbind()))

  }

}
