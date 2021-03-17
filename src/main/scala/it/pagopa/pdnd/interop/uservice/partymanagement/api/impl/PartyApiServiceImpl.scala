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
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Organization, PartyRelationShip, Person, Problem}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class PartyApiServiceImpl(commander: ActorRef[Command]) extends PartyApiService {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Code: 201, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: ErrorResponse
    */
  override def createOrganization(
    organization: Organization
  )(implicit toEntityMarshallerErrorResponse: ToEntityMarshaller[Problem]): Route = {
    logger.info(s"Creating organization ${organization.name}")
    val party: Party = Party.createFromApi(Left(organization))

    val result: Future[StatusReply[PartyPersistentBehavior.State]] = commander.ask(ref => AddParty(party, ref))

    manageCreationResponse(result, createOrganization201, createOrganization400)

  }

  /** Code: 200, Message: successful operation
    * Code: 404, Message: Organization not found
    */
  override def existsOrganization(organizationId: String): Route = {
    logger.info(s"Verify organization ${organizationId}")
    val result: Future[StatusReply[Option[Party]]] = commander.ask(ref => GetParty(organizationId, ref))

    onSuccess(result) { statusReply =>
      statusReply.getValue.fold(existsOrganization404)(party =>
        Party
          .convertToApi(party)
          .swap
          .fold(_ => existsOrganization404, _ => existsOrganization200)
      )
    }

  }

  /** Code: 200, Message: successful operation, DataType: Organization
    * Code: 404, Message: Organization not found, DataType: ErrorResponse
    */
  override def getOrganization(organizationId: String)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info(s"Retrieve organization ${organizationId}")
    val result: Future[StatusReply[Option[Party]]] = commander.ask(ref => GetParty(organizationId, ref))

    val errorResponse: Problem = Problem(detail = None, status = 404, title = "some error")
    onSuccess(result) { statusReply =>
      statusReply.getValue.fold(getOrganization404(errorResponse))(party =>
        Party
          .convertToApi(party)
          .swap
          .fold(_ => getOrganization404(errorResponse), institution => getOrganization200(institution))
      )
    }

  }

  /** Code: 201, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: ErrorResponse
    */
  override def createPerson(
    person: Person
  )(implicit toEntityMarshallerErrorResponse: ToEntityMarshaller[Problem]): Route = {
    logger.info(s"Creating person ${person.name}/${person.surname}")
    val party: Party = Party.createFromApi(Right(person))

    val result: Future[StatusReply[PartyPersistentBehavior.State]] = commander.ask(ref => AddParty(party, ref))

    manageCreationResponse(result, createPerson201, createPerson400)

  }

  /** Code: 200, Message: Person exists
    * Code: 404, Message: Person not found
    */
  override def existsPerson(taxCode: String): Route = {
    logger.info(s"Verify person $taxCode")
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
    toEntityMarshallerErrorResponse: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info(s"Retrieving person $taxCode")
    val result: Future[StatusReply[Option[Party]]] = commander.ask(ref => GetParty(taxCode, ref))

    val errorResponse: Problem = Problem(detail = None, status = 404, title = "some error")
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
  )(implicit toEntityMarshallerErrorResponse: ToEntityMarshaller[Problem]): Route = {
    logger.info(s"Creating reletionship ${partyRelationShip.toString}")
    val result = for {
      from <- commander.ask(ref => GetParty(partyRelationShip.from, ref))
      _ = logger.info(s"From retrieved ${from.toString()}")
      to <- commander.ask(ref => GetParty(partyRelationShip.to, ref))
      _ = logger.info(s"To retrieved ${to.toString()}")
      parties <- extractParties(from, to)
      _ = logger.info(s"Parties retrieved ${parties.toString()}")
      role <- PartyRole.fromText(partyRelationShip.role)
      res  <- commander.ask(ref => AddPartyRelationShip(parties._1, parties._2, role, ref))
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
    failure: Problem => Route
  ): Route = {
    onComplete(result) {
      case Success(statusReply) if statusReply.isError =>
        logger.error(s"Error trying to create organization: ${statusReply.getError.getMessage}")
        failure(Problem(detail = Option(statusReply.getError.getMessage), status = 400, title = "some error"))
      case Success(_) =>
        logger.info(s"Organization successfully created")
        success
      case Failure(ex) =>
        logger.error(s"Error trying to create organization: ${ex.getMessage}")
        failure(Problem(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }
  }
}
