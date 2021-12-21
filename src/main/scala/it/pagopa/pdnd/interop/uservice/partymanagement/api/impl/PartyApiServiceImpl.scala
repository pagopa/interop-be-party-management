package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete, onSuccess}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.pattern.StatusReply
import akka.util.Timeout
import cats.implicits.toTraverseOps
import com.typesafe.scalalogging.Logger
import it.pagopa.pdnd.interop.commons.files.service.FileManager
import it.pagopa.pdnd.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.pdnd.interop.commons.utils.AkkaUtils
import it.pagopa.pdnd.interop.commons.utils.OpenapiUtils._
import it.pagopa.pdnd.interop.commons.utils.TypeConversions._
import it.pagopa.pdnd.interop.commons.utils.service.UUIDSupplier
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PartyApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system._
import it.pagopa.pdnd.interop.uservice.partymanagement.error.{
  OrganizationAlreadyExists,
  OrganizationNotFound,
  RelationshipAlreadyExists,
  TokenNotFound
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence._
import it.pagopa.pdnd.interop.uservice.partymanagement.service.OffsetDateTimeSupplier
import org.slf4j.LoggerFactory

import java.io.File
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class PartyApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]],
  uuidSupplier: UUIDSupplier,
  offsetDateTimeSupplier: OffsetDateTimeSupplier,
  fileManager: FileManager
)(implicit ec: ExecutionContext)
    extends PartyApiService {
  val logger = Logger.takingImplicit[ContextFieldsToLog](LoggerFactory.getLogger(this.getClass))

  private val settings: ClusterShardingSettings = entity.settings match {
    case None    => ClusterShardingSettings(system)
    case Some(s) => s
  }

  def getCommander(entityId: String): EntityRef[Command] =
    sharding.entityRefFor(PartyPersistentBehavior.TypeKey, AkkaUtils.getShard(entityId, settings.numberOfShards))

  /** Code: 200, Message: successful operation
    * Code: 404, Message: Organization not found
    */
  override def existsOrganizationById(id: String): Route = {
    logger.info(s"Verify organization $id")(Seq.empty)

    val result: Future[Option[Party]] = for {
      uuid <- id.toFutureUUID
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

    val commanders = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val organization: Future[StatusReply[Party]] = for {
      shardOrgs <- commanders.traverse(_.ask(ref => GetOrganizationByExternalId(organizationSeed.institutionId, ref)))
      maybeExistingOrg = shardOrgs.flatten.headOption
      newOrg <- maybeExistingOrg
        .toLeft(InstitutionParty.fromApi(organizationSeed, uuidSupplier, offsetDateTimeSupplier))
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
            _ => {
              logger.error(s"Creating organization ${organizationSeed.description} - Bad request")
              createOrganization400(problemOf(StatusCodes.BadRequest, "0001"))
            },
            organization => createOrganization201(organization)
          )
      case Success(_) =>
        val errorResponse: Problem = problemOf(StatusCodes.Conflict, "0002")
        createOrganization409(errorResponse)
      case Failure(ex: OrganizationAlreadyExists) =>
        logger.error(s"Creating organization ${organizationSeed.description}", ex)
        val errorResponse: Problem = problemOf(StatusCodes.Conflict, "0003", ex)
        createOrganization409(errorResponse)
      case Failure(ex) =>
        logger.error(s"Creating organization ${organizationSeed.description}", ex)
        val errorResponse: Problem = problemOf(StatusCodes.BadRequest, "0004", ex)
        createOrganization400(errorResponse)
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
    logger.info(s"Adding attributes to organization {}", organizationId)
    val result: Future[StatusReply[Party]] = for {
      uuid <- organizationId.toFutureUUID
      r    <- getCommander(organizationId).ask(ref => AddAttributes(uuid, requestBody, ref))
    } yield r

    onSuccess(result) {
      case statusReply if statusReply.isSuccess =>
        Party
          .convertToApi(statusReply.getValue)
          .swap
          .fold(
            _ => {
              logger.error("Error adding attributes to organization {} - Bad request", organizationId)
              addOrganizationAttributes404(problemOf(StatusCodes.BadRequest, "0005"))
            },
            organization => addOrganizationAttributes200(organization)
          )
      case statusReply =>
        logger.error("Error adding attributes to organization {} - Not found", organizationId)
        addOrganizationAttributes404(problemOf(StatusCodes.NotFound, "0006", statusReply.getError))
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
    logger.info("Creating person {}", personSeed.id.toString)

    val party: Party = PersonParty.fromApi(personSeed, offsetDateTimeSupplier)

    val result: Future[StatusReply[Party]] =
      getCommander(party.id.toString).ask(ref => AddParty(party, ref))

    onComplete(result) {
      case Success(statusReply) if statusReply.isSuccess =>
        Party
          .convertToApi(statusReply.getValue)
          .fold(
            _ => {
              logger.error("Creating person {} - Bad request", personSeed.id.toString)
              createPerson400(problemOf(StatusCodes.BadRequest, "0007"))
            },
            person => createPerson201(person)
          )
      case Success(_) =>
        logger.error("Creating person {} - Conflict", personSeed.id.toString)
        val errorResponse: Problem = problemOf(StatusCodes.Conflict, "0008")
        createPerson409(errorResponse)
      case Failure(ex) =>
        logger.error("Creating person {}", personSeed.id.toString, ex)
        val errorResponse: Problem = problemOf(StatusCodes.BadRequest, "0009", ex)
        createPerson400(errorResponse)

    }

  }

  /** Code: 200, Message: Person exists
    * Code: 404, Message: Person not found
    */
  override def existsPersonById(id: String): Route = {
    logger.info("Verify if person with the following id exists: {}", id)(Seq.empty)

    val result: Future[Option[Party]] = for {
      uuid <- id.toFutureUUID
      r    <- getCommander(id).ask(ref => GetParty(uuid, ref))
    } yield r

    onSuccess(result) {
      case Some(party) => Party.convertToApi(party).fold(_ => existsPersonById404, _ => existsPersonById200)
      case None =>
        logger.error("Error while verifying if person with the following id exists {} - Not found", id)(Seq.empty)
        existsPersonById404
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
    logger.info("Creating relationship")
    val commanders = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val result: Future[StatusReply[PersistedPartyRelationship]] = for {
      from <- getParty(seed.from)
      to   <- getParty(seed.to)
      role = PersistedPartyRole.fromApi(seed.role)
      _ <- isMissingRelationship(from.id, to.id, role, seed.product)
      partyRelationship = PersistedPartyRelationship.create(uuidSupplier, offsetDateTimeSupplier)(
        from.id,
        to.id,
        role,
        seed.product
      )
      currentPartyRelationships <- commanders
        .traverse(
          _.ask(ref =>
            GetPartyRelationshipsByTo(
              to = to.id,
              roles = List.empty,
              states = List.empty,
              products = List.empty,
              productRoles = List.empty,
              ref
            )
          )
        )
        .map(_.flatten)
      verified          <- isRelationshipAllowed(currentPartyRelationships, partyRelationship)
      partyRelationship <- getCommander(from.id.toString).ask(ref => AddPartyRelationship(verified, ref))
    } yield partyRelationship

    onComplete(result) {
      case Success(statusReply) if statusReply.isSuccess => createRelationship201(statusReply.getValue.toRelationship)
      case Success(statusReply) =>
        logger.error("Error while creating relationship - Conflict", statusReply.getError)
        createRelationship409(problemOf(StatusCodes.Conflict, "0010", statusReply.getError))
      case Failure(ex: RelationshipAlreadyExists) =>
        logger.error("Error while creating relationship", ex)
        createRelationship409(problemOf(StatusCodes.Conflict, "0036", ex))
      case Failure(ex) =>
        logger.error("Error while creating relationship", ex)
        createRelationship400(problemOf(StatusCodes.BadRequest, "0011", ex))
    }

  }

  /** Code: 200, Message: successful operation, DataType: Relationships
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def getRelationships(
    from: Option[String],
    to: Option[String],
    roles: String,
    states: String,
    products: String,
    productRoles: String
  )(implicit
    toEntityMarshallerRelationships: ToEntityMarshaller[Relationships],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {

    logger.info(s"Getting relationships for from: ${from.getOrElse("Empty")} / to: ${to
      .getOrElse("Empty")}/ roles: $roles/ states: $states/ products: $products/ productRoles: $productRoles")

    def retrieveRelationshipsByTo(
      id: UUID,
      roles: List[PartyRole],
      states: List[RelationshipState],
      product: List[String],
      productRoles: List[String]
    ): Future[List[Relationship]] = {
      val commanders: List[EntityRef[Command]] = (0 until settings.numberOfShards)
        .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
        .toList

      for {
        re <- commanders.traverse(
          _.ask[List[PersistedPartyRelationship]](ref =>
            GetPartyRelationshipsByTo(id, roles, states, product, productRoles, ref)
          )
        )
      } yield re.flatten.map(_.toRelationship)
    }

    def retrieveRelationshipsByFrom(
      id: UUID,
      roles: List[PartyRole],
      states: List[RelationshipState],
      product: List[String],
      productRoles: List[String]
    ): Future[List[Relationship]] =
      for {
        re <- getCommander(id.toString).ask(ref =>
          GetPartyRelationshipsByFrom(id, roles, states, product, productRoles, ref)
        )
      } yield re.map(_.toRelationship)

    def relationshipsFromParams(
      from: Option[UUID],
      to: Option[UUID],
      roles: List[PartyRole],
      states: List[RelationshipState],
      product: List[String],
      productRoles: List[String]
    ): Future[List[Relationship]] = (from, to) match {
      case (Some(f), Some(t)) =>
        retrieveRelationshipsByFrom(f, roles, states, product, productRoles).map(_.filter(_.to == t))
      case (Some(f), None) => retrieveRelationshipsByFrom(f, roles, states, product, productRoles)
      case (None, Some(t)) => retrieveRelationshipsByTo(t, roles, states, product, productRoles)
      case _               => Future.failed(new RuntimeException("At least one query parameter between [from, to] must be passed"))
    }

    val result: Future[List[Relationship]] = for {
      fromUuid    <- from.traverse(_.toFutureUUID)
      toUuid      <- to.traverse(_.toFutureUUID)
      rolesArray  <- parseArrayParameters(roles).traverse(PartyRole.fromValue).toFuture
      statesArray <- parseArrayParameters(states).traverse(RelationshipState.fromValue).toFuture
      productsArray     = parseArrayParameters(products)
      productRolesArray = parseArrayParameters(productRoles)
      r <- relationshipsFromParams(fromUuid, toUuid, rolesArray, statesArray, productsArray, productRolesArray)
    } yield r

    onComplete(result) {
      case Success(relationships) => getRelationships200(Relationships(relationships))
      case Failure(ex) =>
        logger.error("Error while getting relationships", ex)
        getRelationships400(problemOf(StatusCodes.BadRequest, "0012", ex))
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
    logger.info("Creating token {}", tokenSeed.toString)

    val result: Future[StatusReply[TokenText]] = for {
      partyRelationships <- Future.traverse(tokenSeed.relationships.items)(getPartyRelationship)
      token              <- Token.generate(tokenSeed, partyRelationships, offsetDateTimeSupplier.get).toFuture
      tokenTxt           <- getCommander(tokenSeed.id).ask(ref => AddToken(token, ref))
    } yield tokenTxt

    manageCreationResponse(result, createToken201, createToken400)

  }

  /** Code: 200, Message: successful operation, DataType: TokenInfo
    * Code: 404, Message: Token not found, DataType: Problem
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def getToken(tokenId: String)(implicit
    toEntityMarshallerTokenInfo: ToEntityMarshaller[TokenInfo],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info("Getting token {}", tokenId)
    val result: Future[TokenInfo] = for {
      tokenIdUUID <- tokenId.toFutureUUID
      result      <- getCommander(tokenId).ask(ref => GetToken(tokenIdUUID, ref))
      token       <- result.toFuture(TokenNotFound(tokenId))
    } yield TokenInfo(id = token.id, checksum = token.checksum, legals = token.legals.map(_.toApi))

    onComplete(result) {
      case Success(token) =>
        getToken200(token)
      case Failure(ex: TokenNotFound) =>
        logger.error("Getting token failed", ex)
        getToken404(problemOf(StatusCodes.NotFound, "0013", ex, "Token not found"))
      case Failure(ex) =>
        logger.error("Getting token failed", ex)
        getToken400(problemOf(StatusCodes.BadRequest, "0014", ex))
    }
  }

  /** Code: 201, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def consumeToken(tokenId: String, doc: (FileInfo, File))(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info("Consuming token {}", tokenId)
    val results: Future[Seq[StatusReply[Unit]]] = for {
      tokenIdUUID <- tokenId.toFutureUUID
      found       <- getCommander(tokenId).ask(ref => GetToken(tokenIdUUID, ref))
      token       <- found.toFuture(TokenNotFound(tokenId))
      results <-
        if (token.isValid) confirmRelationships(token, doc)
        else processRelationships(token, RejectPartyRelationship)
    } yield results

    onComplete(results) {
      case Success(statusReplies) if statusReplies.exists(_.isError) =>
        val errors: String =
          statusReplies.filter(_.isError).flatMap(sr => Option(sr.getError.getMessage)).mkString("\n")
        logger.error("Consuming token failed: {}", errors)
        consumeToken400(problemOf(StatusCodes.BadRequest, "0015", defaultMessage = errors))
      case Success(_) => consumeToken201
      case Failure(ex) =>
        logger.error("Consuming token failed", ex)
        consumeToken400(problemOf(StatusCodes.BadRequest, "0016", ex))
    }

  }

  /** Code: 201, Message: successful operation
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    */
  override def invalidateToken(
    tokenId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {
    logger.info("Invalidating token {}", tokenId)
    val results: Future[Seq[StatusReply[Unit]]] = for {
      tokenIdUUID <- tokenId.toFutureUUID
      found       <- getCommander(tokenId).ask(ref => GetToken(tokenIdUUID, ref))
      token       <- found.toFuture(TokenNotFound(tokenId))
      results     <- processRelationships(token, RejectPartyRelationship)
    } yield results

    onComplete(results) {
      case Success(statusReplies) if statusReplies.exists(_.isError) =>
        val errors: String =
          statusReplies.filter(_.isError).flatMap(sr => Option(sr.getError.getMessage)).mkString("\n")
        logger.error("Invalidating token failed: {}", errors)
        invalidateToken400(problemOf(StatusCodes.BadRequest, "0017", defaultMessage = errors))
      case Success(_) => invalidateToken200
      case Failure(ex) =>
        logger.error("Invalidating token failed", ex)
        invalidateToken400(problemOf(StatusCodes.BadRequest, "0018", ex))
    }

  }

  private def getParty(id: UUID)(implicit ec: ExecutionContext, timeout: Timeout): Future[Party] = for {
    found <- getCommander(id.toString).ask(ref => GetParty(id, ref))
    party <- found.fold(Future.failed[Party](new RuntimeException(s"Party ${id.toString} not found")))(p =>
      Future.successful(p)
    )
  } yield party

  private def getPartyRelationship(relationship: Relationship): Future[PersistedPartyRelationship] =
    relationshipByInvolvedParties(
      from = relationship.from,
      to = relationship.to,
      role = PersistedPartyRole.fromApi(relationship.role),
      productId = relationship.product.id,
      productRole = relationship.product.role
    )

  private def isRelationshipAllowed(
    currentPartyRelationships: List[PersistedPartyRelationship],
    partyRelationships: PersistedPartyRelationship
  ): Future[PersistedPartyRelationship] = Future.fromTry {
    Either
      .cond(
        currentPartyRelationships.exists(_.state == PersistedPartyRelationshipState.Active) ||
          Set[PersistedPartyRole](Manager, Delegate).contains(partyRelationships.role),
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
      filePath <- fileManager.store(ApplicationConfiguration.storageContainer)(token.id, fileParts)
      results <- Future.traverse(token.legals) { partyRelationshipBinding =>
        getCommander(partyRelationshipBinding.partyId.toString).ask((ref: ActorRef[StatusReply[Unit]]) =>
          ConfirmPartyRelationship(partyRelationshipBinding.relationshipId, filePath, fileParts._1, token.id, ref)
        )
      } //TODO atomic?
    } yield results
  }

  private def manageCreationResponse[A](result: Future[StatusReply[A]], success: A => Route, failure: Problem => Route)(
    implicit contexts: Seq[(String, String)]
  ): Route = {
    onComplete(result) {
      case Success(statusReply) if statusReply.isError =>
        logger.error("Error trying to create element", statusReply.getError)
        failure(problemOf(StatusCodes.BadRequest, "0021", statusReply.getError))
      case Success(a) =>
        logger.info(s"Element successfully created")
        success(a.getValue)
      case Failure(ex) =>
        logger.error("Error trying to create element", ex)
        failure(problemOf(StatusCodes.BadRequest, "0022", ex))
    }
  }

  /** Code: 200, Message: Party Attributes, DataType: Seq[String]
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Party not found, DataType: Problem
    */
  override def getPartyAttributes(
    id: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {
    logger.info("Getting party {} attributes", id)
    val attributes: Future[StatusReply[Seq[String]]] = for {
      uuid <- id.toFutureUUID
      r    <- getCommander(id).ask(ref => GetPartyAttributes(uuid, ref))
    } yield r

    onComplete(attributes) {
      case Success(result) if result.isSuccess =>
        getPartyAttributes200(result.getValue)
      case Success(_) =>
        val error = problemOf(StatusCodes.InternalServerError, "0019")
        logger.error("Error while getting party {} attributes - Internal server error", id)
        complete((error.status, error))
      case Failure(ex) =>
        logger.error("Error while getting party {} attributes", id, ex)
        getPartyAttributes404(problemOf(StatusCodes.NotFound, "0020", ex, "Party not found"))
    }
  }

  /* does a recursive lookup through the shards until it finds the existing relationship for the involved parties */
  private def relationshipByInvolvedParties(
    from: UUID,
    to: UUID,
    role: PersistedPartyRole,
    productId: String,
    productRole: String
  ): Future[PersistedPartyRelationship] = {
    for {
      maybeRelationship <- getCommander(from.toString).ask(ref =>
        GetPartyRelationshipByAttributes(
          from = from,
          to = to,
          role = role,
          product = productId,
          productRole = productRole,
          ref
        )
      )
      result <- maybeRelationship.toFuture(new RuntimeException("Relationship not found"))
    } yield result

  }

  /** flips result of relationship retrieval from the cluster.
    *
    * @return successful future if no relationship has been found in the cluster.
    */
  private def isMissingRelationship(
    from: UUID,
    to: UUID,
    role: PersistedPartyRole,
    product: RelationshipProductSeed
  ): Future[Boolean] = {
    relationshipByInvolvedParties(from, to, role, product.id, product.role).transformWith {
      case Success(relationship) => Future.failed(RelationshipAlreadyExists(relationship.id))
      case Failure(_)            => Future.successful(true)
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
    logger.info("Getting organization with id {}", id)
    def notFound: Route = getOrganizationById404(
      problemOf(StatusCodes.NotFound, "0023", defaultMessage = s"Organization $id Not Found")
    )

    val organizations = for {
      organizationUUID <- id.toFutureUUID
      results          <- getCommander(id).ask(ref => GetParty(organizationUUID, ref))
    } yield results

    onComplete(organizations) {
      case Success(result) =>
        result
          .fold(notFound) { reply =>
            Party.convertToApi(reply).swap.fold(_ => notFound, p => getOrganizationById200(p))
          }
      case Failure(ex) =>
        logger.error("Error getting organization with id {}", id, ex)
        getOrganizationById404(problemOf(StatusCodes.NotFound, "0024", ex, "Organization not found"))
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
    logger.info("Getting person with id {}", id)
    def notFound: Route = getPersonById404(
      problemOf(StatusCodes.NotFound, "0025", defaultMessage = s"Person $id Not Found")
    )

    val persons = for {
      personUUID <- id.toFutureUUID
      results    <- getCommander(id).ask(ref => GetParty(personUUID, ref))
    } yield results

    onComplete(persons) {
      case Success(result) =>
        result.fold(notFound) { reply =>
          Party.convertToApi(reply).fold(_ => notFound, p => getPersonById200(p))
        }
      case Failure(ex) =>
        logger.error("Error while getting person with id {}", id, ex)
        getPersonById404(problemOf(StatusCodes.NotFound, "0026", ex, s"Person Not Found"))
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
    logger.info("Getting relationship with id {}", relationshipId)

    val commanders = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val result: Future[Option[Relationship]] =
      for {
        uuid    <- relationshipId.toFutureUUID
        results <- commanders.traverse(_.ask(ref => GetPartyRelationshipById(uuid, ref)))
        maybePartyRelationship = results.find(_.isDefined).flatten
        partyRelationship      = maybePartyRelationship.map(_.toRelationship)
      } yield partyRelationship

    onComplete(result) {
      case Success(Some(relationship)) => getRelationshipById200(relationship)
      case Success(None) =>
        logger.error("Error while getting relationship with id {} - Not found", relationshipId)
        getRelationshipById404(problemOf(StatusCodes.NotFound, "0027"))
      case Failure(ex) =>
        logger.error("Error while getting relationship with id {}", relationshipId, ex)
        getRelationshipById400(problemOf(StatusCodes.BadRequest, "0028", ex))
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
    logger.info("Bulk organizations")

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
        logger.error("Error while processing bulk organizations", ex)
        bulkOrganizations404(problemOf(StatusCodes.NotFound, "0029", ex))
    }

  }

  /** Code: 204, Message: Relationship activated
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Relationship not found, DataType: Problem
    */
  override def activatePartyRelationshipById(
    relationshipId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {
    logger.info("Activating relationship with id {}", relationshipId)
    val commanders = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val result = for {
      uuid <- relationshipId.toFutureUUID
      resultsCollection <- Future.traverse(commanders)(
        _.ask(ref => ActivatePartyRelationship(uuid, ref)).transform(Success(_))
      )
      _ <- resultsCollection.reduce((r1, r2) => if (r1.isSuccess) r1 else r2).toFuture
    } yield ()

    onComplete(result) {
      case Success(_) =>
        activatePartyRelationshipById204
      case Failure(ex) =>
        logger.error("Error while activating relationship with id {}", relationshipId, ex)
        activatePartyRelationshipById404(problemOf(StatusCodes.NotFound, "0030", ex, "Relationship not found"))
    }
  }

  /** Code: 204, Message: Relationship suspended
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Relationship not found, DataType: Problem
    */
  override def suspendPartyRelationshipById(
    relationshipId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {
    logger.info("Suspending relationship with id {}", relationshipId)
    val commanders = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val result = for {
      uuid <- relationshipId.toFutureUUID
      resultsCollection <- Future.traverse(commanders)(
        _.ask(ref => SuspendPartyRelationship(uuid, ref)).transform(Success(_))
      )
      _ <- resultsCollection.reduce((r1, r2) => if (r1.isSuccess) r1 else r2).toFuture
    } yield ()

    onComplete(result) {
      case Success(_) =>
        suspendPartyRelationshipById204
      case Failure(ex) =>
        logger.error("Error while suspending relationship with id {}", relationshipId, ex)
        suspendPartyRelationshipById404(problemOf(StatusCodes.NotFound, "0031", ex, "Relationship not found"))
    }
  }

  override def getOrganizationByExternalId(externalId: String)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info("Getting organization with external id {}", externalId)

    val commanders = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val organization: Future[InstitutionParty] = for {
      shardOrgs <- commanders.traverse(_.ask(ref => GetOrganizationByExternalId(externalId, ref)))
      result    <- shardOrgs.flatten.headOption.toFuture(OrganizationNotFound(externalId))
    } yield result

    onComplete(organization) {
      case Success(statusReply) =>
        Party
          .convertToApi(statusReply)
          .swap
          .fold(
            _ => {
              logger.error("Error while getting organization with external id {} - Not found", externalId)
              getOrganizationByExternalId400(
                problemOf(StatusCodes.BadRequest, "0032", defaultMessage = "Relationship not found")
              )
            },
            organization => getOrganizationByExternalId200(organization)
          )
      case Failure(ex) =>
        logger.error("Error while getting organization with external id {}", externalId, ex)
        getOrganizationByExternalId400(problemOf(StatusCodes.BadRequest, "0033", ex))
    }

  }

  /** Code: 204, Message: relationship deleted
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 404, Message: Relationship not found, DataType: Problem
    */
  override def deleteRelationshipById(
    relationshipId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {
    logger.info("Deleting relationship with id {}", relationshipId)

    val commanders = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val result: Future[Option[StatusReply[Unit]]] =
      for {
        uuid    <- relationshipId.toFutureUUID
        results <- commanders.traverse(_.ask(ref => DeletePartyRelationship(uuid, ref)))
        maybeDeletion = results.find(_.isSuccess)
      } yield maybeDeletion

    onComplete(result) {
      case Success(Some(reply)) =>
        if (reply.isSuccess) {
          deleteRelationshipById204
        } else {
          logger.error("Error while deleting relationship with id {} - Not found", relationshipId)
          deleteRelationshipById404(
            problemOf(
              StatusCodes.NotFound,
              "0032",
              defaultMessage = s"Error while deleting relationship $relationshipId"
            )
          )
        }
      case Success(None) =>
        logger.error("Error while deleting relationship with id {} - Not found", relationshipId)
        deleteRelationshipById404(problemOf(StatusCodes.NotFound, "0034"))
      case Failure(ex) =>
        logger.error("Error while deleting relationship with id {}", relationshipId, ex)
        deleteRelationshipById400(problemOf(StatusCodes.BadRequest, "0035", ex))
    }
  }
}
