package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.{onComplete, onSuccess}
import akka.http.scaladsl.server.Route
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PartyApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.{ApiParty, executionContext, scheduler, timeout}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils._
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{PartyRelationShip => _, _}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence._
import it.pagopa.pdnd.interop.uservice.partymanagement.service.UUIDSupplier
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.Future
import scala.util.{Failure, Success}

class PartyApiServiceImpl(commander: ActorRef[Command], uuidSupplier: UUIDSupplier) extends PartyApiService {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Code: 200, Message: successful operation
    * Code: 404, Message: Organization not found
    */
  override def existsOrganization(organizationId: String): Route = {
    logger.info(s"Verify organization $organizationId")
    val result: Future[StatusReply[Option[ApiParty]]] = commander.ask(ref => GetParty(organizationId, ref))

    onSuccess(result) { statusReply =>
      statusReply.getValue.fold(existsOrganization404)(party =>
        party.swap
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
    logger.info(s"Retrieve organization $organizationId")
    val result = commander.ask(ref => GetParty(organizationId, ref))

    val errorResponse: Problem = Problem(detail = None, status = 404, title = "some error")
    onSuccess(result) { statusReply =>
      statusReply.getValue.fold(getOrganization404(errorResponse))(party =>
        party.swap
          .fold(_ => getOrganization404(errorResponse), institution => getOrganization200(institution))
      )
    }

  }

  /** Code: 201, Message: successful operation, DataType: Organization
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def createOrganization(organizationSeed: OrganizationSeed)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info(s"Creating organization ${organizationSeed.description}")
    val party: Party = InstitutionParty.fromApi(organizationSeed, uuidSupplier)

    val result: Future[StatusReply[ApiParty]] = commander.ask(ref => AddParty(party, ref))

    onSuccess(result) {
      case statusReply if statusReply.isSuccess =>
        statusReply.getValue.swap
          .fold(
            _ => createOrganization400(Problem(detail = None, status = 400, title = "some error")),
            organization => createOrganization201(organization)
          )
      case statusReply =>
        createOrganization400(
          Problem(detail = Option(statusReply.getError.getMessage), status = 400, title = "some error")
        )
    }

  }

  /** Code: 200, Message: successful operation, DataType: Organization
    * Code: 404, Message: Organization not found, DataType: Problem
    */
  override def addOrganizationAttributes(organizationId: String, attributeRecord: Seq[AttributeRecord])(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {

    val result = commander.ask(ref => AddAttributes(organizationId, attributeRecord, ref))

    onSuccess(result) {
      case statusReply if statusReply.isSuccess =>
        statusReply.getValue.swap
          .fold(
            _ => createOrganization400(Problem(detail = None, status = 400, title = "some error")),
            organization => createOrganization201(organization)
          )
      case statusReply =>
        createOrganization400(
          Problem(detail = Option(statusReply.getError.getMessage), status = 400, title = "some error")
        )
    }
  }

  /** Code: 201, Message: successful operation, DataType: Person
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def createPerson(personSeed: PersonSeed)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info(s"Creating person ${personSeed.name}/${personSeed.surname}")
    val party: Party = PersonParty.fromApi(personSeed, uuidSupplier)

    val result: Future[StatusReply[ApiParty]] = commander.ask(ref => AddParty(party, ref))

    onSuccess(result) {
      case statusReply if statusReply.isSuccess =>
        statusReply.getValue
          .fold(
            _ => createPerson400(Problem(detail = None, status = 400, title = "some error")),
            person => createPerson201(person)
          )
      case statusReply =>
        createPerson400(Problem(detail = Option(statusReply.getError.getMessage), status = 400, title = "some error"))
    }

  }

  /** Code: 200, Message: Person exists
    * Code: 404, Message: Person not found
    */
  override def existsPerson(taxCode: String): Route = {
    logger.info(s"Verify person $taxCode")
    val result: Future[StatusReply[Option[ApiParty]]] = commander.ask(ref => GetParty(taxCode, ref))

    onSuccess(result) { statusReply =>
      statusReply.getValue.fold(existsPerson404)(party => party.fold(_ => existsPerson404, _ => existsPerson200))
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
    val result: Future[StatusReply[Option[ApiParty]]] = commander.ask(ref => GetParty(taxCode, ref))

    val errorResponse: Problem = Problem(detail = None, status = 404, title = "some error")

    onSuccess(result) { statusReply =>
      statusReply.getValue.fold(getPerson404(errorResponse))(party =>
        party.fold(_ => getPerson404(errorResponse), person => getPerson200(person))
      )
    }

  }

  /** Code: 201, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: ErrorResponse
    */
  override def createRelationShip(
    relationShip: RelationShip
  )(implicit toEntityMarshallerErrorResponse: ToEntityMarshaller[Problem]): Route = {
    logger.info(s"Creating relationship ${relationShip.toString}")
    val result: Future[StatusReply[State]] = for {
      from <- commander.ask(ref => GetParty(relationShip.from, ref))
      _ = logger.info(s"From retrieved ${from.toString()}")
      to <- commander.ask(ref => GetParty(relationShip.to, ref))
      _ = logger.info(s"To retrieved ${to.toString()}")
      parties <- extractParties(from, to)
      _ = logger.info(s"Parties retrieved ${parties.toString()}")
      role <- PartyRole.fromText(relationShip.role).toFuture
      res <- commander.ask(ref =>
        AddPartyRelationShip(UUID.fromString(parties._1.partyId), UUID.fromString(parties._2.partyId), role, ref)
      )
    } yield res

    onComplete(result) {
      case Success(statusReply) if statusReply.isError =>
        createRelationShip400(
          Problem(detail = Option(statusReply.getError.getMessage), status = 404, title = "some error")
        )
      case Success(_) => createRelationShip201
      case Failure(ex) =>
        createRelationShip400(Problem(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }

  }

  /** Code: 200, Message: successful operation, DataType: RelationShips
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def getRelationShips(from: String)(implicit
    toEntityMarshallerRelationShips: ToEntityMarshaller[RelationShips],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {

    logger.info(s"Getting relationships for $from")
    val result: Future[StatusReply[List[RelationShip]]] =
      commander.ask(ref => GetPartyRelationShips(UUID.fromString(from), ref))

    onComplete(result) {
      case Success(statusReply) if statusReply.isError =>
        getRelationShips400(
          Problem(detail = Option(statusReply.getError.getMessage), status = 400, title = "some error")
        )
      case Success(result) if result.getValue.isEmpty =>
        getRelationShips404(Problem(detail = None, status = 404, title = "some error"))
      case Success(result) => getRelationShips200(RelationShips(result.getValue))
      case Failure(ex) =>
        getRelationShips400(Problem(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }

  }

  /** Code: 201, Message: successful operation, DataType: TokenText
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def createToken(tokenSeed: TokenSeed)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTokenText: ToEntityMarshaller[TokenText]
  ): Route = {
    logger.info(s"Creating token ${tokenSeed.toString}")

    val result: Future[StatusReply[TokenText]] = commander.ask(ref => AddToken(tokenSeed, ref))

    manageCreationResponse(result, createToken201, createToken400)

  }

  /** Code: 200, Message: successful operation
    * Code: 404, Message: Token not found, DataType: Problem
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def verifyToken(token: String)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route = {
    val result: Future[StatusReply[Option[Token]]] = for {
      token <- Future.fromTry(Token.decode(token))
      res   <- commander.ask(ref => VerifyToken(token, ref))
    } yield res

    onComplete(result) {
      case Success(statusReply) if statusReply.isError =>
        verifyToken400(Problem(detail = Option(statusReply.getError.getMessage), status = 400, title = "some error"))
      case Success(token) if token.getValue.nonEmpty => verifyToken200
      case Success(_)                                => verifyToken404(Problem(detail = None, status = 404, title = "Token not found"))
      case Failure(ex) =>
        verifyToken400(Problem(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }
  }

  /** Code: 201, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def consumeToken(token: String)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route = {
    val result: Future[StatusReply[State]] = for {
      token <- Future.fromTry(Token.decode(token))
      res <-
        if (token.isValid) commander.ask(ref => ConsumeToken(token, ref))
        else commander.ask(ref => InvalidateToken(token, ref))
    } yield res

    onComplete(result) {
      case Success(statusReply) if statusReply.isError =>
        consumeToken400(Problem(detail = Option(statusReply.getError.getMessage), status = 400, title = "some error"))
      case Success(_) => consumeToken201
      case Failure(ex) =>
        consumeToken400(Problem(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }

  }

  /** Code: 201, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def invalidateToken(
    token: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route = {
    val result: Future[StatusReply[State]] = for {
      token <- Future.fromTry(Token.decode(token))
      res   <- commander.ask(ref => InvalidateToken(token, ref))
    } yield res

    onComplete(result) {
      case Success(statusReply) if statusReply.isError =>
        invalidateToken400(
          Problem(detail = Option(statusReply.getError.getMessage), status = 404, title = "some error")
        )
      case Success(_) => invalidateToken201
      case Failure(ex) =>
        invalidateToken400(Problem(detail = Option(ex.getMessage), status = 404, title = "some error"))
    }

  }

  //TODO Improve this part
  private def extractParties(
    from: StatusReply[Option[ApiParty]],
    to: StatusReply[Option[ApiParty]]
  ): Future[(Person, Organization)] =
    Future.fromTry {

      if (from.isError || to.isError)
        Failure(new RuntimeException("Party extraction from ApiParty failed"))
      else {
        val parties: Option[(Person, Organization)] = for {
          apiPartyFrom <- from.getValue
          person       <- apiPartyFrom.toOption
          apiPartyTo   <- to.getValue
          organization <- apiPartyTo.swap.toOption
        } yield (person, organization)

        parties.map(Success(_)).getOrElse(Failure(new RuntimeException("Party extraction from ApiParty failed")))

      }
    }

  private def manageCreationResponse[A](
    result: Future[StatusReply[A]],
    success: A => Route,
    failure: Problem => Route
  ): Route = {
    onComplete(result) {
      case Success(statusReply) if statusReply.isError =>
        logger.error(s"Error trying to create element: ${statusReply.getError.getMessage}")
        failure(Problem(detail = Option(statusReply.getError.getMessage), status = 400, title = "some error"))
      case Success(a) =>
        logger.info(s"Element successfully created")
        success(a.getValue)
      case Failure(ex) =>
        logger.error(s"Error trying to create element: ${ex.getMessage}")
        failure(Problem(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }
  }
}
