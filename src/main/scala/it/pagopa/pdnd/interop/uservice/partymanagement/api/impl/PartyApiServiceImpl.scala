package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.{onSuccess, onComplete}
import akka.http.scaladsl.server.Route
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PartyApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.{ApiParty, scheduler, timeout}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.Converter
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.Party
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.Party._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyPersistentBehavior
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyPersistentBehavior.{
  AddParty,
  Command,
  GetParty
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{ErrorResponse, Institution, PartyRelationShip, Person}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class PartyApiServiceImpl(commander: ActorRef[Command]) extends PartyApiService {

  override def createPerson(person: Person)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {

    val party: Party = Converter.convert[ApiParty](Right(person))

    val result: Future[StatusReply[PartyPersistentBehavior.State]] = commander.ask(ref => AddParty(party, ref))

    onComplete(result) {
      case Success(_) => createPerson201(person)
      case Failure(ex) =>
        createPerson400(ErrorResponse(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }
  }

  override def createInstitution(institution: Institution)(implicit
    toEntityMarshallerInstitution: ToEntityMarshaller[Institution],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {

    val party: Party = Converter.convert[ApiParty](Left(institution))

    val result: Future[StatusReply[PartyPersistentBehavior.State]] = commander.ask(ref => AddParty(party, ref))

    onComplete(result) {
      case Success(_) => createInstitution(institution)
      case Failure(ex) =>
        createInstitution400(ErrorResponse(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }
  }

  override def existsInstitution(institutionId: String): Route = {
    val result: Future[StatusReply[Option[Party]]] = commander.ask(ref => GetParty(institutionId, ref))

    onSuccess(result) { statusReply =>
      statusReply.getValue.fold(existsInstitution404)(party =>
        Converter
          .convert[Party](party)
          .swap
          .fold(_ => existsInstitution404, _ => existsInstitution200)
      )
    }

  }

  override def existsPerson(taxCode: String): Route = {
    val result: Future[StatusReply[Option[Party]]] = commander.ask(ref => GetParty(taxCode, ref))

    onSuccess(result) { statusReply =>
      statusReply.getValue.fold(existsInstitution404)(party =>
        Converter
          .convert[Party](party)
          .fold(_ => existsPerson404, _ => existsInstitution200)
      )

    }
  }

  override def createRelationShip(taxCode: String, institutionId: String)(implicit
    toEntityMarshallerPartyRelationShip: ToEntityMarshaller[PartyRelationShip]
  ): Route = ???
}
