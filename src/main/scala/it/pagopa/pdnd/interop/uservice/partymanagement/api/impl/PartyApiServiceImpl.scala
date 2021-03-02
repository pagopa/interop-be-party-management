package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.actor.typed.{ActorRef, Scheduler}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import akka.pattern.StatusReply
import akka.util.Timeout
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PartyApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.actorSystem
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.Party
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyPersistentBehavior
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyPersistentBehavior.{Add, Command}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{ErrorResponse, Institution, PartyRelationShip, Person}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class PartyApiServiceImpl(commander: ActorRef[Command]) extends PartyApiService {

  implicit val timeout: Timeout                           = 3.seconds
  implicit val scheduler: Scheduler                       = actorSystem.scheduler
  implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext

  override def createPerson(person: Person)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {

    val party: Either[Throwable, Party] = Party(person)

    val result: Future[StatusReply[PartyPersistentBehavior.State]] =
      party.fold(ex => Future.failed(ex), p => commander.ask(ref => Add(p, ref)))

    onComplete(result) {
      case Success(_)  => createPerson200(person)
      case Failure(ex) => createPerson400(ErrorResponse(Some(ex.getMessage), None, None, None, None))
    }
  }

  override def createRelationShip(taxCode: String, institutionId: String)(implicit
    toEntityMarshallerPartyRelationShip: ToEntityMarshaller[PartyRelationShip]
  ): Route = ???

  override def createInstitution(institution: Institution)(implicit
    toEntityMarshallerInstitution: ToEntityMarshaller[Institution],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getInstitutionByID(institutionId: String)(implicit
    toEntityMarshallerInstitution: ToEntityMarshaller[Institution],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getPersonByTaxCode(taxCode: String)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???
}
