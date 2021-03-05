package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.{onComplete, onSuccess}
import akka.http.scaladsl.server.Route
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PartyApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.{executionContext, scheduler, timeout}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{Party, PartyRole}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyPersistentBehavior
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyPersistentBehavior.{
  AddParty,
  AddPartyRelationShip,
  Command,
  GetParty
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{ErrorResponse, Institution, PartyRelationShip, Person}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class PartyApiServiceImpl(commander: ActorRef[Command]) extends PartyApiService {

  override def createPerson(
    person: Person
  )(implicit toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]): Route = {

    val party: Party = Party.createFromApi(Right(person))

    val result: Future[StatusReply[PartyPersistentBehavior.State]] = commander.ask(ref => AddParty(party, ref))

    onComplete(result) {
      case Success(_) => createPerson201
      case Failure(ex) =>
        createPerson400(ErrorResponse(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }
  }

  override def createInstitution(
    institution: Institution
  )(implicit toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]): Route = {

    val party: Party = Party.createFromApi(Left(institution))

    val result: Future[StatusReply[PartyPersistentBehavior.State]] = commander.ask(ref => AddParty(party, ref))

    onComplete(result) {
      case Success(_) => createInstitution201
      case Failure(ex) =>
        createInstitution400(ErrorResponse(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }
  }

  override def existsInstitution(institutionId: String): Route = {
    val result: Future[StatusReply[Option[Party]]] = commander.ask(ref => GetParty(institutionId, ref))

    onSuccess(result) { statusReply =>
      statusReply.getValue.fold(existsInstitution404)(party =>
        Party
          .convertToApi(party)
          .swap
          .fold(_ => existsInstitution404, _ => existsInstitution200)
      )
    }

  }

  override def existsPerson(taxCode: String): Route = {
    val result: Future[StatusReply[Option[Party]]] = commander.ask(ref => GetParty(taxCode, ref))

    onSuccess(result) { statusReply =>
      statusReply.getValue.fold(existsInstitution404)(party =>
        Party
          .convertToApi(party)
          .fold(_ => existsPerson404, _ => existsInstitution200)
      )

    }
  }

  override def createRelationShip(
    partyRelationShip: PartyRelationShip
  )(implicit toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]): Route = {

    val result = for {
      from    <- commander.ask(ref => GetParty(partyRelationShip.from, ref))
      to      <- commander.ask(ref => GetParty(partyRelationShip.to, ref))
      parties <- extractParties(from, to)
      role    <- PartyRole(partyRelationShip.role)
      res     <- commander.ask(ref => AddPartyRelationShip(parties._1, parties._2, role, ref))
    } yield res

    onComplete(result) {
      case Success(_) => createRelationShip201
      case Failure(ex) =>
        createPerson400(ErrorResponse(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }
  }

  //TODO Improve this part
  private def extractParties(
    from: StatusReply[Option[Party]],
    to: StatusReply[Option[Party]]
  ): Future[(Party, Party)] = {
    Future.fromTry(Try((from.getValue.get, to.getValue.get)))

  }
}
