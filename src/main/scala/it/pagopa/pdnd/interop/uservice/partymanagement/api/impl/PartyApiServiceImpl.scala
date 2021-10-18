package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.{onComplete, onSuccess}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.pattern.StatusReply
import akka.util.Timeout
import cats.implicits.toTraverseOps
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PartyApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system._
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils._
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence._
import it.pagopa.pdnd.interop.uservice.partymanagement.service.UUIDSupplier
import it.pagopa.pdnd.interop.uservice.partymanagement.service._
import org.slf4j.{Logger, LoggerFactory}

import java.io.File
import java.util.UUID
import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@SuppressWarnings(
  Array("org.wartremover.warts.Nothing", "org.wartremover.warts.OptionPartial", "org.wartremover.warts.TraversableOps")
)
class PartyApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]],
  uuidSupplier: UUIDSupplier,
  fileManager: FileManager
)(implicit ec: ExecutionContext)
    extends PartyApiService {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val settings: ClusterShardingSettings = entity.settings match {
    case None    => ClusterShardingSettings(system)
    case Some(s) => s
  }

  def getCommander(entityId: String): EntityRef[Command] =
    sharding.entityRefFor(PartyPersistentBehavior.TypeKey, getShard(entityId))

  @inline private def getShard(id: String): String = Math.abs(id.hashCode % settings.numberOfShards).toString

  /** Code: 200, Message: successful operation
    * Code: 404, Message: Organization not found
    */
  override def existsOrganization(organizationId: String): Route = {
    logger.info(s"Verify organization $organizationId")

    val result: Future[Option[Party]] =
      getCommander(organizationId).ask(ref => GetPartyByExternalId(organizationId, getShard(organizationId), ref))

    onSuccess(result) {
      case Some(party) => Party.convertToApi(party).swap.fold(_ => existsOrganization404, _ => existsOrganization200)
      case None        => existsOrganization404
    }

  }

  /** Code: 200, Message: successful operation, DataType: Organization
    * Code: 404, Message: Organization not found, DataType: ErrorResponse
    */
  override def getOrganization(organizationId: String)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info(s"Retrieve organization $organizationId")

    val result: Future[Option[Party]] =
      getCommander(organizationId).ask(ref => GetPartyByExternalId(organizationId, getShard(organizationId), ref))

    val errorResponse: Problem = Problem(detail = None, status = 404, title = "some error")

    onSuccess(result) {
      case Some(party) =>
        Party.convertToApi(party).swap.fold(_ => getOrganization404(errorResponse), o => getOrganization200(o))
      case None => getOrganization404(errorResponse)
    }

  }

  /** Code: 201, Message: successful operation, DataType: Organization
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def createOrganization(organizationSeed: OrganizationSeed)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info(s"Creating organization ${organizationSeed.description}")
    val party: Party = InstitutionParty.fromApi(organizationSeed, uuidSupplier)

    val result: Future[StatusReply[Party]] =
      getCommander(party.externalId).ask(ref => AddParty(party, getShard(party.externalId), ref))

    onSuccess(result) {
      case statusReply if statusReply.isSuccess =>
        Party
          .convertToApi(statusReply.getValue)
          .swap
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
  override def addOrganizationAttributes(organizationId: String, requestBody: Seq[String])(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {

    val result: Future[StatusReply[Party]] =
      getCommander(organizationId).ask(ref => AddAttributes(organizationId, requestBody, ref))

    onSuccess(result) {
      case statusReply if statusReply.isSuccess =>
        Party
          .convertToApi(statusReply.getValue)
          .swap
          .fold(
            _ => addOrganizationAttributes404(Problem(detail = None, status = 400, title = "some error")),
            organization => addOrganizationAttributes200(organization)
          )
      case statusReply =>
        addOrganizationAttributes404(
          Problem(detail = Option(statusReply.getError.getMessage), status = 404, title = "some error")
        )
    }

  }

  /** Code: 201, Message: successful operation, DataType: Person
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def createPerson(personSeed: PersonSeed)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info(s"Creating person ${personSeed.name}/${personSeed.surname}")

    val party: Party = PersonParty.fromApi(personSeed, uuidSupplier)

    val result: Future[StatusReply[Party]] =
      getCommander(party.externalId).ask(ref => AddParty(party, getShard(party.externalId), ref))

    onSuccess(result) {
      case statusReply if statusReply.isSuccess =>
        Party
          .convertToApi(statusReply.getValue)
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

    val result: Future[Option[Party]] =
      getCommander(taxCode).ask(ref => GetPartyByExternalId(taxCode, getShard(taxCode), ref))

    onSuccess(result) {
      case Some(party) => Party.convertToApi(party).fold(_ => existsPerson404, _ => existsPerson200)
      case None        => existsPerson404
    }

  }

  /** Code: 200, Message: Person exists, DataType: Person
    * Code: 404, Message: Person not found, DataType: ErrorResponse
    */
  override def getPerson(taxCode: String)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info(s"Retrieving person $taxCode")

    val result: Future[Option[Party]] =
      getCommander(taxCode).ask(ref => GetPartyByExternalId(taxCode, getShard(taxCode), ref))

    val errorResponse: Problem = Problem(detail = None, status = 404, title = "some error")

    onSuccess(result) {
      case Some(party) => Party.convertToApi(party).fold(_ => getPerson404(errorResponse), p => getPerson200(p))
      case None        => getPerson404(errorResponse)
    }

  }

  /** Code: 201, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def createRelationship(seed: RelationshipSeed)(implicit
    toEntityMarshallerRelationship: ToEntityMarshaller[Relationship],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {

    val commanders = (0 to settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val result: Future[Relationship] = for {
      from <- getParty(seed.from)
      to   <- getParty(seed.to)
      role <- PartyRole.fromText(seed.role).toFuture
      _    <- isMissingRelationship(from.id, to.id, role, seed.platformRole)
      partyRelationship = PartyRelationship.create(uuidSupplier)(from.id, to.id, role, seed.platformRole)
      currentPartyRelationships <- commanders.getPartyRelationships(to.id, GetPartyRelationshipsByTo)
      verified                  <- isRelationshipAllowed(currentPartyRelationships, partyRelationship)
      _                         <- getCommander(from.id.toString).ask(ref => AddPartyRelationship(verified, ref))
      relationship = Relationship(
        id = verified.id,
        from = from.externalId,
        to = to.externalId,
        role = verified.role.toString,
        platformRole = verified.platformRole,
        status = verified.status.toString,
        filePath = None
      )
    } yield relationship

    onComplete(result) {
      case Success(relationship) => createRelationship201(relationship)
      case Failure(ex) =>
        createRelationship400(Problem(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }

  }

  /** Code: 200, Message: successful operation, DataType: Relationships
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def getRelationships(from: Option[String], to: Option[String], platformRole: Option[String])(implicit
    toEntityMarshallerRelationships: ToEntityMarshaller[Relationships],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {

    logger.error(
      s"Getting relationships for ${from.getOrElse("Empty")}/${to.getOrElse("Empty")}/${platformRole.getOrElse("Empty")}"
    )

    val commanders: List[EntityRef[Command]] = (0 to settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    def retrieveRelationships(
      externalId: String,
      commandFunc: (UUID, ActorRef[List[PartyRelationship]]) => PartyRelationshipCommand
    ): Future[List[Relationship]] = for {
      party <- getParty(externalId)
      re    <- commanders.getRelationships(party, commandFunc)
    } yield re

    val result: Future[List[Relationship]] = (from, to) match {
      case (Some(f), Some(t)) => retrieveRelationships(f, GetPartyRelationshipsByFrom).map(_.filter(_.to == t))
      case (Some(f), None)    => retrieveRelationships(f, GetPartyRelationshipsByFrom)
      case (None, Some(t))    => retrieveRelationships(t, GetPartyRelationshipsByTo)
      case _                  => Future.failed(new RuntimeException("At least one query parameter between [from, to] must be passed"))
    }

    val filteredResult = platformRole match {
      case Some(pr) => result.map(_.filter(_.platformRole == pr))
      case None     => result
    }

    onComplete(filteredResult) {
      case Success(relationships) => getRelationships200(Relationships(relationships))
      case Failure(ex) =>
        getRelationships400(Problem(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }

  }

  /** Code: 201, Message: successful operation, DataType: TokenText
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def createToken(tokenSeed: TokenSeed)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTokenText: ToEntityMarshaller[TokenText],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info(s"Creating token ${tokenSeed.toString}")

    val result: Future[StatusReply[TokenText]] = for {
      partyRelationships <- Future.traverse(tokenSeed.relationships.items)(getPartyRelationship)
      token              <- getCommander(tokenSeed.seed).ask(ref => AddToken(tokenSeed, partyRelationships, ref))
    } yield token

    manageCreationResponse(result, createToken201, createToken400)

  }

  /** Code: 200, Message: successful operation
    * Code: 404, Message: Token not found, DataType: Problem
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def verifyToken(
    token: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {

    val result: Future[StatusReply[Option[Token]]] = for {
      token <- Future.fromTry(Token.decode(token))
      res   <- getCommander(token.seed.toString).ask(ref => VerifyToken(token, ref))
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
  override def consumeToken(token: String, doc: (FileInfo, File))(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {

    val results: Future[Seq[StatusReply[Unit]]] = for {
      token <- Future.fromTry(Token.decode(token))
      results <-
        if (token.isValid) confirmRelationships(token, doc)
        else processRelationships(token, DeletePartyRelationship)
    } yield results

    onComplete(results) {
      case Success(statusReplies) if statusReplies.exists(_.isError) =>
        val errors: String =
          statusReplies.filter(_.isError).flatMap(sr => Option(sr.getError.getMessage)).mkString("\n")
        consumeToken400(Problem(detail = Option(errors), status = 400, title = "some error"))
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
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {
    val results: Future[Seq[StatusReply[Unit]]] = for {
      token   <- Future.fromTry(Token.decode(token))
      results <- processRelationships(token, DeletePartyRelationship)
    } yield results

    onComplete(results) {
      case Success(statusReplies) if statusReplies.exists(_.isError) =>
        val errors: String =
          statusReplies.filter(_.isError).flatMap(sr => Option(sr.getError.getMessage)).mkString("\n")
        invalidateToken400(Problem(detail = Option(errors), status = 400, title = "some error"))
      case Success(_) => invalidateToken200
      case Failure(ex) =>
        invalidateToken400(Problem(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }

  }

  private def getParty(externalId: String)(implicit ec: ExecutionContext, timeout: Timeout): Future[Party] = for {
    found <- getCommander(externalId).ask(ref => GetPartyByExternalId(externalId, getShard(externalId), ref))
    party <- found.fold(Future.failed[Party](new RuntimeException(s"Party $externalId not found")))(p =>
      Future.successful(p)
    )
  } yield party

  private def getPartyRelationship(relationshipSeed: RelationshipSeed): Future[PartyRelationship] = for {
    from <- getCommander(relationshipSeed.from).ask(ref =>
      GetPartyByExternalId(relationshipSeed.from, getShard(relationshipSeed.from), ref)
    )
    to <- getCommander(relationshipSeed.to).ask(ref =>
      GetPartyByExternalId(relationshipSeed.to, getShard(relationshipSeed.to), ref)
    )
    role = PartyRole.fromText(relationshipSeed.role).toOption
    relationship <- relationshipByInvolvedParties(
      from = from.get.id,
      to = to.get.id,
      role = role.get,
      platformRole = relationshipSeed.platformRole
    )
  } yield relationship

  private def isRelationshipAllowed(
    currentPartyRelationships: List[PartyRelationship],
    partyRelationships: PartyRelationship
  ): Future[PartyRelationship] = Future.fromTry {
    Either
      .cond(
        currentPartyRelationships.exists(_.status == PartyRelationshipStatus.Active) ||
          Set[PartyRole](Manager, Delegate).contains(partyRelationships.role),
        partyRelationships,
        new RuntimeException("Operator without active manager")
      )
      .toTry
  }

  private def processRelationships(
    token: Token,
    commandFunc: (UUID, ActorRef[StatusReply[Unit]]) => Command
  ): Future[Seq[StatusReply[Unit]]] = {
    for {
      results <- Future.traverse(token.legals) { partyRelationshipBinding =>
        getCommander(partyRelationshipBinding.partyId.toString).ask((ref: ActorRef[StatusReply[Unit]]) =>
          commandFunc(partyRelationshipBinding.relationshipId, ref)
        )
      } //TODO atomic?
    } yield results
  }

  private def confirmRelationships(token: Token, fileParts: (FileInfo, File)): Future[Seq[StatusReply[Unit]]] = {
    for {
      filePath <- fileManager.store(token.seed, fileParts)
      results <- Future.traverse(token.legals) { partyRelationshipBinding =>
        getCommander(partyRelationshipBinding.partyId.toString).ask((ref: ActorRef[StatusReply[Unit]]) =>
          ConfirmPartyRelationship(partyRelationshipBinding.relationshipId, filePath, ref)
        )
      } //TODO atomic?
    } yield results
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

  /** Code: 200, Message: Party Attributes, DataType: Seq[String]
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Party not found, DataType: Problem
    */
  override def getPartyAttributes(
    id: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {

    val commanders = (0 to settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val attributes: Future[List[StatusReply[Seq[String]]]] = Future.traverse(commanders) { commander =>
      commander.ask(ref => GetPartyAttributes(UUID.fromString(id), ref)) //TODO make this UUID better
    }

    onComplete(attributes) {
      case Success(result) =>
        result
          .find(_.isSuccess)
          .fold(getPartyAttributes404(Problem(Option(s"Party $id Not Found"), status = 404, "party not found"))) {
            reply =>
              getPartyAttributes200(reply.getValue)
          }
      case Failure(ex) => getPartyAttributes404(Problem(Option(ex.getMessage), status = 404, "party not found"))
    }
  }

  /* does a recursive lookup through the shards until it finds the existing relationship for the involved parties */
  private def relationshipByInvolvedParties(
    from: UUID,
    to: UUID,
    role: PartyRole,
    platformRole: String
  ): Future[PartyRelationship] = {
    val commanders: List[EntityRef[Command]] =
      (0 until settings.numberOfShards)
        .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
        .toList

    recursiveLookup(commanders, from, to, role, platformRole) match {
      case Some(relationship) => Future.successful(relationship)
      case None               => Future.failed(new RuntimeException("Relationship not found"))
    }
  }

  @tailrec
  private def recursiveLookup(
    commanders: List[EntityRef[Command]],
    from: UUID,
    to: UUID,
    role: PartyRole,
    platformRole: String
  ): Option[PartyRelationship] = {
    commanders match {
      case Nil => None
      case elem :: tail =>
        Await.result(
          elem.ask(ref =>
            GetPartyRelationshipByAttributes(from = from, to = to, role = role, platformRole = platformRole, ref)
          ),
          Duration.Inf
        ) match {
          case Some(relationship) => Some(relationship)
          case None               => recursiveLookup(tail, from, to, role, platformRole)
        }
    }
  }

  /** flips result of relationship retrieval from the cluster.
    *
    * @return successful future if no relationship has been found in the cluster.
    */
  private def isMissingRelationship(from: UUID, to: UUID, role: PartyRole, platformRole: String): Future[Boolean] = {
    relationshipByInvolvedParties(from, to, role, platformRole).transformWith {
      case Success(_) => Future.failed(new RuntimeException("Relationship already existing"))
      case Failure(_) => Future.successful(true)
    }
  }

  /** Code: 200, Message: Organization, DataType: Organization
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Organization not found, DataType: Problem
    */
  override def getPartyOrganizationByUUID(id: String)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {

    def notFound = getPartyOrganizationByUUID404(
      Problem(Option(s"Organization $id Not Found"), status = 404, "organization not found")
    )

    val commanders = (0 to settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val organizations = for {
      organizationUUID <- id.asUUID.toFuture
      results          <- Future.traverse(commanders) { commander => commander.ask(ref => GetParty(organizationUUID, ref)) }
    } yield results

    onComplete(organizations) {
      case Success(result) =>
        result.flatten
          .find(_.id.toString == id)
          .fold(notFound) { reply =>
            Party.convertToApi(reply).swap.fold(_ => notFound, p => getPartyOrganizationByUUID200(p))
          }
      case Failure(ex) =>
        getPartyOrganizationByUUID404(Problem(Option(ex.getMessage), status = 404, "organization not found"))
    }
  }

  /** Code: 200, Message: Person, DataType: Person
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Person not found, DataType: Problem
    */
  override def getPartyPersonByUUID(id: String)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {

    def notFound = getPartyPersonByUUID404(Problem(Option(s"Person $id Not Found"), status = 404, "person not found"))

    val commanders = (0 to settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val persons = for {
      personUUID <- id.asUUID.toFuture
      results    <- Future.traverse(commanders) { commander => commander.ask(ref => GetParty(personUUID, ref)) }
    } yield results

    onComplete(persons) {
      case Success(result) =>
        result.flatten
          .find(_.id.toString == id)
          .fold(notFound) { reply =>
            Party.convertToApi(reply).fold(_ => notFound, p => getPartyPersonByUUID200(p))
          }
      case Failure(ex) =>
        getPartyPersonByUUID404(Problem(Option(ex.getMessage), status = 404, "person not found"))
    }
  }

  /** Code: 200, Message: successful operation, DataType: Relationship
    * Code: 404, Message: Relationship not found, DataType: Problem
    */
  override def getRelationshipById(relationshipId: String)(implicit
    toEntityMarshallerRelationship: ToEntityMarshaller[Relationship],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info(s"Retrieving relationship $relationshipId")

    val commanders = (0 to settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val result: Future[Option[Relationship]] =
      for {
        uuid    <- Try(UUID.fromString(relationshipId)).toEither.toFuture
        results <- commanders.traverse(_.ask(ref => GetPartyRelationshipById(uuid, ref)))
        maybePartyRelationship = results.find(_.isDefined).flatten
        partyRelationship <- maybePartyRelationship.flatTraverse(commanders.convertToRelationship)
      } yield partyRelationship

    onComplete(result) {
      case Success(Some(relationship)) => getRelationshipById200(relationship)
      case Success(None)               => getRelationshipById404(Problem(detail = None, status = 404, title = "some error"))
      case Failure(ex) =>
        getRelationshipById400(Problem(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }
  }

  /** Code: 200, Message: array of organizations, DataType: Seq[Organization]
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Organization not found, DataType: Problem
    */
  override def bulkOrganizations(bulkPartiesSeed: BulkPartiesSeed)(implicit
    toEntityMarshallerBulkOrganizations: ToEntityMarshaller[BulkOrganizations],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {

    val commanders = (0 to settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val result: Future[Seq[Party]] = for {
      results <- Future.traverse(commanders) { commander =>
        commander.ask(ref => GetParties(bulkPartiesSeed.partyIdentifiers, ref))
      }
    } yield results.flatten

    onComplete(result) {
      case Success(replies) =>
        val organizations: Seq[Organization] =
          replies.flatMap(p => Party.convertToApi(p).swap.fold(_ => None, org => Some(org)))

        val response = BulkOrganizations(
          found = organizations,
          notFound = bulkPartiesSeed.partyIdentifiers.map(_.toString).diff(organizations.map(_.partyId))
        )
        bulkOrganizations200(response)
      case Failure(ex) =>
        bulkOrganizations404(Problem(detail = Option(ex.getMessage), status = 400, title = "some error"))
    }

  }

  /** Code: 204, Message: Relationship activated
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Relationship not found, DataType: Problem
    */
  override def activatePartyRelationshipById(
    relationshipId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {

    val commanders = (0 to settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val result = for {
      uuid <- relationshipId.asUUID.toFuture
      resultsCollection <- Future.traverse(commanders)(
        _.ask(ref => ActivatePartyRelationship(uuid, ref)).transform(Success(_))
      )
      _ <- resultsCollection.reduce((r1, r2) => if (r1.isSuccess) r1 else r2).toFuture
    } yield ()

    onComplete(result) {
      case Success(_) =>
        activatePartyRelationshipById204
      case Failure(ex) =>
        activatePartyRelationshipById404(Problem(Option(ex.getMessage), status = 404, "Relationship not found"))
    }
  }

  /** Code: 204, Message: Relationship suspended
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Relationship not found, DataType: Problem
    */
  override def suspendPartyRelationshipById(
    relationshipId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {

    val commanders = (0 to settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val result = for {
      uuid <- relationshipId.asUUID.toFuture
      resultsCollection <- Future.traverse(commanders)(
        _.ask(ref => SuspendPartyRelationship(uuid, ref)).transform(Success(_))
      )
      _ <- resultsCollection.reduce((r1, r2) => if (r1.isSuccess) r1 else r2).toFuture
    } yield ()

    onComplete(result) {
      case Success(_) =>
        suspendPartyRelationshipById204
      case Failure(ex) =>
        suspendPartyRelationshipById404(Problem(Option(ex.getMessage), status = 404, "Relationship not found"))
    }
  }
}
