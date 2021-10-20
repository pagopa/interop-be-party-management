package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.{complete, onComplete, onSuccess}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.pattern.StatusReply
import akka.util.Timeout
import cats.implicits.toTraverseOps
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PartyApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system._
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils._
import it.pagopa.pdnd.interop.uservice.partymanagement.error.OrganizationAlreadyExists
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence._
import it.pagopa.pdnd.interop.uservice.partymanagement.service.{UUIDSupplier, _}
import org.slf4j.{Logger, LoggerFactory}

import java.io.File
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
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
  override def existsOrganizationById(id: String): Route = {
    logger.info(s"Verify organization $id")

    val result: Future[Option[Party]] = for {
      uuid <- id.asUUID.toFuture
      r    <- getCommander(id).ask(ref => GetParty(uuid, ref))
    } yield r

    onSuccess(result) {
      case Some(party) =>
        Party.convertToApi(party).swap.fold(_ => existsOrganizationById404, _ => existsOrganizationById200)
      case None => existsOrganizationById404
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

    val commanders = (0 to settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val organization: Future[StatusReply[Party]] = for {
      shardOrgs <- commanders.traverse(_.ask(ref => GetOrganizationByExternalId(organizationSeed.institutionId, ref)))
      maybeExistingOrg = shardOrgs.flatten.headOption
      newOrg <- maybeExistingOrg
        .toLeft(InstitutionParty.fromApi(organizationSeed, uuidSupplier))
        .left
        .map(_ => OrganizationAlreadyExists(organizationSeed.institutionId))
        .toFuture
      result <- getCommander(newOrg.id.toString).ask(ref => AddParty(newOrg, ref))
    } yield result

    onComplete(organization) {
      case Success(statusReply) if statusReply.isSuccess =>
        Party
          .convertToApi(statusReply.getValue)
          .swap
          .fold(
            _ => createOrganization400(Problem(detail = None, status = 400, title = "some error")),
            organization => createOrganization201(organization)
          )
      case Success(_) =>
        createOrganization400(Problem(detail = None, status = 400, title = "some error"))
      case Failure(ex) =>
        createOrganization400(Problem(detail = Option(ex.getMessage), status = 400, title = "some error"))
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

    val result: Future[StatusReply[Party]] = for {
      uuid <- organizationId.asUUID.toFuture
      r    <- getCommander(organizationId).ask(ref => AddAttributes(uuid, requestBody, ref))
    } yield r

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
    logger.info(s"Creating person ${personSeed.id.toString}")

    val party: Party = PersonParty.fromApi(personSeed)

    val result: Future[StatusReply[Party]] =
      getCommander(party.id.toString).ask(ref => AddParty(party, ref))

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
  override def existsPersonById(id: String): Route = {
    logger.info(s"Verify person $id")

    val result: Future[Option[Party]] = for {
      uuid <- id.asUUID.toFuture
      r    <- getCommander(id).ask(ref => GetParty(uuid, ref))
    } yield r

    onSuccess(result) {
      case Some(party) => Party.convertToApi(party).fold(_ => existsPersonById404, _ => existsPersonById200)
      case None        => existsPersonById404
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
      currentPartyRelationships <- commanders.traverse(_.ask(GetPartyRelationshipsByTo(to.id, _))).map(_.flatten)
      verified                  <- isRelationshipAllowed(currentPartyRelationships, partyRelationship)
      _                         <- getCommander(from.id.toString).ask(ref => AddPartyRelationship(verified, ref))
      relationship = Relationship(
        id = verified.id,
        from = from.id,
        to = to.id,
        role = verified.role.toString,
        platformRole = verified.platformRole,
        status = verified.status.toString,
        filePath = None,
        fileName = None,
        contentType = None
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

    def retrieveRelationshipsByTo(id: UUID): Future[List[Relationship]] = {
      val commanders: List[EntityRef[Command]] = (0 to settings.numberOfShards)
        .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
        .toList

      for {
        re <- commanders.traverse(_.ask[List[PartyRelationship]](ref => GetPartyRelationshipsByTo(id, ref)))
      } yield re.flatten.map(_.toRelationship)
    }

    def retrieveRelationshipsByFrom(id: UUID): Future[List[Relationship]] =
      for {
        re <- getCommander(id.toString).ask(ref => GetPartyRelationshipsByFrom(id, ref))
      } yield re.map(_.toRelationship)

    def relationshipsFromParams(from: Option[UUID], to: Option[UUID]): Future[List[Relationship]] = (from, to) match {
      case (Some(f), Some(t)) => retrieveRelationshipsByFrom(f).map(_.filter(_.to == t))
      case (Some(f), None)    => retrieveRelationshipsByFrom(f)
      case (None, Some(t))    => retrieveRelationshipsByTo(t)
      case _                  => Future.failed(new RuntimeException("At least one query parameter between [from, to] must be passed"))
    }

    val result: Future[List[Relationship]] = for {
      fromUuid <- from.traverse(_.asUUID.toFuture)
      toUuid   <- to.traverse(_.asUUID.toFuture)
      r        <- relationshipsFromParams(fromUuid, toUuid)
    } yield r

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

  private def getParty(id: UUID)(implicit ec: ExecutionContext, timeout: Timeout): Future[Party] = for {
    found <- getCommander(id.toString).ask(ref => GetParty(id, ref))
    party <- found.fold(Future.failed[Party](new RuntimeException(s"Party ${id.toString} not found")))(p =>
      Future.successful(p)
    )
  } yield party

  private def getPartyRelationship(relationshipSeed: RelationshipSeed): Future[PartyRelationship] = for {
    role <- PartyRole.fromText(relationshipSeed.role).toFuture
    relationship <- relationshipByInvolvedParties(
      from = relationshipSeed.from,
      to = relationshipSeed.to,
      role = role,
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
          ConfirmPartyRelationship(partyRelationshipBinding.relationshipId, filePath, fileParts._1, ref)
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

    val attributes: Future[StatusReply[Seq[String]]] = for {
      uuid <- id.asUUID.toFuture
      r    <- getCommander(id).ask(ref => GetPartyAttributes(uuid, ref))
    } yield r

    onComplete(attributes) {
      case Success(result) if result.isSuccess =>
        getPartyAttributes200(result.getValue)
      case Success(_) =>
        complete((500, Problem(None, status = 500, "Unexpected error")))
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

    for {
      maybeRelationship <- getCommander(from.toString).ask(ref =>
        GetPartyRelationshipByAttributes(from = from, to = to, role = role, platformRole = platformRole, ref)
      )
      result <- maybeRelationship.toFuture(new RuntimeException("Relationship not found"))
    } yield result

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
  override def getOrganizationById(id: String)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {

    def notFound: Route = getOrganizationById404(
      Problem(Option(s"Organization $id Not Found"), status = 404, "organization not found")
    )

    val organizations = for {
      organizationUUID <- id.asUUID.toFuture
      results          <- getCommander(id).ask(ref => GetParty(organizationUUID, ref))
    } yield results

    onComplete(organizations) {
      case Success(result) =>
        result
          .fold(notFound) { reply =>
            Party.convertToApi(reply).swap.fold(_ => notFound, p => getOrganizationById200(p))
          }
      case Failure(ex) =>
        getOrganizationById404(Problem(Option(ex.getMessage), status = 404, "organization not found"))
    }
  }

  /** Code: 200, Message: Person, DataType: Person
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Person not found, DataType: Problem
    */
  override def getPersonById(id: String)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {

    def notFound: Route = getPersonById404(Problem(Option(s"Person $id Not Found"), status = 404, "person not found"))

    val persons = for {
      personUUID <- id.asUUID.toFuture
      results    <- getCommander(id).ask(ref => GetParty(personUUID, ref))
    } yield results

    onComplete(persons) {
      case Success(result) =>
        result.fold(notFound) { reply =>
          Party.convertToApi(reply).fold(_ => notFound, p => getPersonById200(p))
        }
      case Failure(ex) =>
        getPersonById404(Problem(Option(ex.getMessage), status = 404, "person not found"))
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
        partyRelationship      = maybePartyRelationship.map(_.toRelationship)
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

    def getParty(id: UUID): Future[Option[Party]] =
      getCommander(id.toString).ask(ref => GetParty(id, ref))

    val result = bulkPartiesSeed.partyIdentifiers.traverse(getParty)

    onComplete(result) {
      case Success(replies) =>
        val organizations: Seq[Organization] =
          replies.flatten.flatMap(p => Party.convertToApi(p).swap.fold(_ => None, org => Some(org)))

        val response = BulkOrganizations(
          found = organizations,
          notFound = bulkPartiesSeed.partyIdentifiers.diff(organizations.map(_.id)).map(_.toString)
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
