package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.{onComplete, onSuccess}
import akka.http.scaladsl.server.Route
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PartyApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.{ApiParty, executionContext, scheduler, timeout}
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

  /** Code: 201, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: ErrorResponse
    */
  override def createInstitution(
    institution: Institution
  )(implicit toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]): Route = {

    val party: Party = Party.createFromApi(Left(institution))

    val result: Future[StatusReply[PartyPersistentBehavior.State]] = commander.ask(ref => AddParty(party, ref))

    manageCreationResponse(result, createInstitution201, createInstitution400)

  }

  /** Code: 200, Message: successful operation
    * Code: 404, Message: Institution not found
    */
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

  /** Code: 200, Message: successful operation, DataType: Institution
    * Code: 404, Message: Institution not found, DataType: ErrorResponse
    */
  override def getInstitution(institutionId: String)(implicit
    toEntityMarshallerInstitution: ToEntityMarshaller[Institution],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result: Future[StatusReply[Option[Party]]] = commander.ask(ref => GetParty(institutionId, ref))

    val errorResponse: ErrorResponse = ErrorResponse(detail = None, status = 404, title = "some error")
    onSuccess(result) { statusReply =>
      statusReply.getValue.fold(getInstitution404(errorResponse))(party =>
        Party
          .convertToApi(party)
          .swap
          .fold(_ => getInstitution404(errorResponse), institution => getInstitution200(institution))
      )
    }

  }

  /** Code: 201, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: ErrorResponse
    */
  override def createPerson(
    person: Person
  )(implicit toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]): Route = {

    val party: Party = Party.createFromApi(Right(person))

    val result: Future[StatusReply[PartyPersistentBehavior.State]] = commander.ask(ref => AddParty(party, ref))

    manageCreationResponse(result, createPerson201, createPerson400)

  }

  /** Code: 200, Message: Person exists
    * Code: 404, Message: Person not found
    */
  override def existsPerson(taxCode: String): Route = {
    val result: Future[StatusReply[Option[Party]]] = commander.ask(ref => GetParty(taxCode, ref))

    onSuccess(result) { statusReply =>
      statusReply.getValue.fold(existsPerson404)(party =>
        Party
          .convertToApi(party)
          .fold(_ => existsPerson404, _ => existsPerson200)
      )
    }

  }

  /** Code: 200, Message: Person exists, DataType: Person
    * Code: 404, Message: Person not found, DataType: ErrorResponse
    */
  override def getPerson(taxCode: String)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {

    val result: Future[StatusReply[Option[Party]]] = commander.ask(ref => GetParty(taxCode, ref))

    val errorResponse: ErrorResponse = ErrorResponse(detail = None, status = 404, title = "some error")
    onSuccess(result) { statusReply =>
      statusReply.getValue.fold(getPerson404(errorResponse))(party =>
        Party
          .convertToApi(party)
          .fold(_ => getPerson404(errorResponse), person => getPerson200(person))
      )
    }
  }

  /** Code: 201, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: ErrorResponse
    */
  override def createRelationShip(
    partyRelationShip: PartyRelationShip
  )(implicit toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]): Route = {

    val result = for {
      from    <- commander.ask(ref => GetParty(partyRelationShip.from, ref))
      to      <- commander.ask(ref => GetParty(partyRelationShip.to, ref))
      parties <- extractParties(from, to)
      role    <- PartyRole.fromText(partyRelationShip.role)
      res     <- commander.ask(ref => AddPartyRelationShip(parties._1, parties._2, role, ref))
    } yield res

    manageCreationResponse(result, createRelationShip201, createPerson400)

  }

  //TODO Improve this part
  private def extractParties(
    from: StatusReply[Option[Party]],
    to: StatusReply[Option[Party]]
  ): Future[(Party, Party)] = {
    Future.fromTry(Try((from.getValue.get, to.getValue.get)))
  }

  private def manageCreationResponse[A](
    result: Future[StatusReply[A]],
    success: Route,
    failure: ErrorResponse => Route
  ): Route = {
    onComplete(result) {
      case Success(statusReply) if statusReply.isError =>
        failure(ErrorResponse(detail = Option(statusReply.getError.getMessage), status = 400, title = "some error"))
      case Success(_)  => success
      case Failure(ex) => failure(ErrorResponse(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }
  }
}
