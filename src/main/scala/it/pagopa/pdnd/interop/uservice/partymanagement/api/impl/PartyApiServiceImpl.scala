package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PartyApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.{scheduler, timeout}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.Converter
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{Party, PersonParty}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyPersistentBehavior
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyPersistentBehavior.{Add, Command, Get}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{ErrorResponse, Institution, PartyRelationShip, Person}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class PartyApiServiceImpl(commander: ActorRef[Command]) extends PartyApiService {

  override def createPerson(person: Person)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {

    val party: Either[Throwable, Party] = Party(person)

    party.fold(ex => ex.printStackTrace(), c => println(c.toString))

    val result: Future[StatusReply[PartyPersistentBehavior.State]] =
      party.fold(ex => Future.failed(ex), p => commander.ask(ref => Add(p, ref)))

    onComplete(result) {
      case Success(_) => createPerson201(person)
      case Failure(ex) =>
        createPerson400(ErrorResponse(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }
  }

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
  ): Route = {
    val result: Future[StatusReply[Option[Party]]] = commander.ask(ref => Get(taxCode, ref))

    onComplete(result) {
      case Success(statusReply) =>
        statusReply.getValue
          .map {
            case person: PersonParty => getPersonByTaxCode200(Converter.convert(person))
            case _ =>
              getInstitutionByID400(
                ErrorResponse(detail = Option("Invalid person id"), status = 400, title = "some error")
              )
          }
          .getOrElse(
            getInstitutionByID404(ErrorResponse(detail = Option("NotFound"), status = 404, title = "some error"))
          )

      case Failure(ex) =>
        createPerson400(ErrorResponse(detail = Option(ex.getMessage), status = 400, title = "some error"))

    }
  }

  override def createRelationShip(taxCode: String, institutionId: String)(implicit
    toEntityMarshallerPartyRelationShip: ToEntityMarshaller[PartyRelationShip]
  ): Route = ???
}
