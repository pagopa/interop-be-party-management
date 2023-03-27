package it.pagopa.interop.partymanagement.api.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils
import it.pagopa.interop.commons.utils.OpenapiUtils.parseArrayParameters
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.partymanagement.api.NewDesignExposureApiService
import it.pagopa.interop.partymanagement.common.system._
import it.pagopa.interop.partymanagement.error.PartyManagementErrors.{
  FindNewDesignInstitutionError,
  FindNewDesignTokenError,
  FindNewDesignUserError
}
import it.pagopa.interop.partymanagement.model._
import it.pagopa.interop.partymanagement.model.party._
import it.pagopa.interop.partymanagement.model.persistence._
import it.pagopa.interop.partymanagement.service.{InstitutionService, RelationshipService}
import org.slf4j.LoggerFactory

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class NewDesignExposureApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]],
  relationshipService: RelationshipService,
  institutionService: InstitutionService
)(implicit ec: ExecutionContext)
    extends NewDesignExposureApiService {

  val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](LoggerFactory.getLogger(this.getClass))

  private val settings: ClusterShardingSettings = entity.settings match {
    case None    => ClusterShardingSettings(system)
    case Some(s) => s
  }

  val orderedRelationshipStatuses = List(
    RelationshipState.ACTIVE,
    RelationshipState.SUSPENDED,
    RelationshipState.TOBEVALIDATED,
    RelationshipState.PENDING,
    RelationshipState.REJECTED,
    RelationshipState.DELETED
  )

  def getAllCommanders: List[EntityRef[Command]] = {
    val commanders = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList
    commanders
  }

  def getCommander(entityId: String): EntityRef[Command] =
    sharding.entityRefFor(PartyPersistentBehavior.TypeKey, AkkaUtils.getShard(entityId, settings.numberOfShards))

  /**
    * Code: 200, Message: collection of institutions, DataType: Seq[NewDesignUser]
    * Code: 400, Message: Bad Request, DataType: Problem
    */
  override def findNewDesignUsers(userIds: Option[String], page: Int, size: Int)(implicit
    toEntityMarshallerNewDesignUserarray: ToEntityMarshaller[Seq[NewDesignUser]],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val users = for {
      user2relationShips <- userIds
        .map(s => retrieveUser2RelationShips(parseArrayParameters(s)))
        .getOrElse(retrieveUser2RelationShips(page, size))
      newDesignUsers = user2relationShips.map(x => userRelationships2NewDesignUser(x._1, x._2))
    } yield newDesignUsers

    onComplete(users) {
      case Success(result) =>
        findNewDesignUsers200(result)
      case Failure(ex)     =>
        logger.error(
          s"Exporting institutions using new design userIds=$userIds, page=$page, size=$size return an error",
          ex
        )
        val errorResponse: Problem = problemOf(StatusCodes.InternalServerError, FindNewDesignUserError(ex.getMessage))
        findNewDesignUsers500(errorResponse)
    }
  }

  def retrieveUser2RelationShips(userIds: List[String]): Future[List[(UUID, Seq[Relationship])]] =
    relationshipService
      .getRelationshipsByUserIds(userIds.map(_.toUUID).filter(_.isSuccess).map(_.get))
      .map(groupRelationshipsByUserId(_).toList)

  def retrieveUser2RelationShips(page: Int, size: Int): Future[List[(UUID, Seq[Relationship])]] =
    relationshipService
      .getRelationships()
      .map(groupRelationshipsByUserId(_).toList)
      .map(user2Relationships => {
        user2Relationships
          .sortBy(_._1)
          .slice(page * size, page * size + size)
      })

  private def groupRelationshipsByUserId: Seq[Relationship] => Map[UUID, Seq[Relationship]] =
    _.groupBy(_.from)

  private def retrieveTokenId(lastSignedRole: Option[Relationship], rels: Seq[Relationship])(implicit
    contexts: Seq[(String, String)]
  ): Option[String] = {
    lastSignedRole
      .flatMap(_.tokenId.map(_.toString))
      .orElse {
        rels
          .filter(r => r.role == PartyRole.MANAGER || r.role == PartyRole.DELEGATE)
          .flatMap(retrieveTokenIdByRelationship)
          .headOption
      }
  }

  private def retrieveTokenIdByRelationship(
    relationship: Relationship
  )(implicit contexts: Seq[(String, String)]): Option[String] = {
    val commanders: List[EntityRef[Command]] = getAllCommanders

    val resultFuture = for {
      tokens <- Future.traverse(commanders)(_.ask(ref => GetTokensByRelationshipUUID(relationship.id, ref)))
      tokensFlatten = tokens.flatten
      tokenId       = tokensFlatten.maxByOption(_.validity).map(_.id.toString)
    } yield tokenId

    val result = Await.result(resultFuture, Duration.Inf)
    if (result.isEmpty) {
      logger.warn(
        s"Found signed relationship ${relationship.role} having status ${relationship.state} without related token on institution ${relationship.to} and product ${relationship.product.id} relationshipId ${relationship.id} created at ${relationship.createdAt}"
      )
    }
    result
  }

  private def userRelationships2NewDesignUser(uid: UUID, rels: Seq[Relationship])(implicit
    contexts: Seq[(String, String)]
  ): NewDesignUser =
    NewDesignUser(
      id = uid.toString,
      createdAt = rels.map(_.createdAt).min,
      bindings = rels
        .groupBy(_.to)
        .map(institutionUid2Rels =>
          NewDesignUserInstitution(
            institutionId = institutionUid2Rels._1.toString,
            products = institutionUid2Rels._2
              .map(rel => {
                val tokenId = retrieveTokenIdByRelationship(rel)

                NewDesignUserInstitutionProduct(
                  productId = rel.product.id,
                  env = "ROOT",
                  relationshipId = rel.id.toString,
                  role = rel.role,
                  productRole = rel.product.role,
                  status = rel.state,
                  tokenId = tokenId,
                  contract = rel.filePath,
                  createdAt = rel.createdAt,
                  updatedAt = rel.updatedAt
                )
              })
          )
        )
        .toSeq
    )

  def retrieveLastRelationships: Seq[Relationship] => Seq[Relationship] = rels =>
    retrieveRelationshipsHavingFirstState(rels, orderedRelationshipStatuses)

  def retrieveRelationshipsHavingFirstState: (Seq[Relationship], List[RelationshipState]) => Seq[Relationship] =
    (productRels, stateOrder) => {
      if (stateOrder.isEmpty) {
        Seq.empty
      } else {
        val relsHavingHeadState = productRels.filter(_.state == stateOrder.head)

        if (relsHavingHeadState.nonEmpty) {
          relsHavingHeadState
        } else {
          retrieveRelationshipsHavingFirstState(productRels, stateOrder.slice(1, stateOrder.size))
        }
      }
    }

  /**
    * Code: 200, Message: collection of institutions modelled as new design, DataType: Seq[NewDesignInstitution]
    * Code: 500, Message: Error, DataType: Problem
    */
  override def findNewDesignInstitutions(institutionIds: Option[String], page: Int, size: Int)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerNewDesignInstitutionarray: ToEntityMarshaller[Seq[NewDesignInstitution]],
    contexts: Seq[(String, String)]
  ): Route = {
    val institutions: Future[Seq[NewDesignInstitution]] = for {
      institutions          <- institutionIds
        .map(s => institutionService.getInstitutionsByIds(parseArrayParameters(s)))
        .getOrElse(retrieveInstitutions(page, size))
      newDesignInstitutions <- Future.traverse(institutions)(institutionParty2NewDesignInstitution)
    } yield newDesignInstitutions

    onComplete(institutions) {
      case Success(result) =>
        findNewDesignInstitutions200(result)
      case Failure(ex)     =>
        logger.error(
          s"Exporting institutions using new design institutionIds=$institutionIds, page=$page, size=$size return an error",
          ex
        )
        val errorResponse: Problem =
          problemOf(StatusCodes.InternalServerError, FindNewDesignInstitutionError(ex.getMessage))
        findNewDesignInstitutions500(errorResponse)
    }
  }

  def retrieveInstitutions(page: Int, size: Int): Future[Seq[InstitutionParty]] =
    institutionService
      .getInstitutions()
      .map(institutions => {
        institutions
          .sortBy(_.id)
          .slice(page * size, page * size + size)
      })

  private def institutionParty2NewDesignInstitution(
    party: InstitutionParty
  )(implicit contexts: Seq[(String, String)]): Future[NewDesignInstitution] = {

    def warnIfNoContract(lastManager: Relationship): Unit = {
      if (lastManager.filePath.isEmpty && lastManager.state == RelationshipState.ACTIVE) {
        logger.error(
          s"Found ACTIVE manager without a signed contract: userId ${lastManager.from} having status ${lastManager.state} on institutionId ${lastManager.to} and product ${lastManager.product.id} and relationshipId ${lastManager.id} created at ${lastManager.createdAt}"
        )
      }
    }

    for {
      product2managers <- relationshipService
        .retrieveRelationshipsByTo(party.id, List(PartyRole.MANAGER), List.empty, List.empty, List.empty)
        .map(_.groupBy(_.product.id))

      onboardedProducts = party.products
        .map(p => {
          val productManagers = product2managers(p.product)
          val lastManager     = retrieveLastRelationships(productManagers).head

          warnIfNoContract(lastManager)

          val tokenId = retrieveTokenId(Some(lastManager), productManagers)

          NewDesignInstitutionOnboarding(
            productId = p.product,
            status = lastManager.state,
            tokenId = tokenId,
            contract = lastManager.filePath,
            pricingPlan = p.pricingPlan,
            billing = p.billing.toBilling.copy(publicServices = p.billing.publicServices.orElse(Some(false))),
            createdAt = productManagers.map(_.createdAt).min,
            updatedAt = lastManager.updatedAt
          )
        })

      onboardedProductIds = onboardedProducts.map(_.productId)

      pendingProducts = product2managers
        .filter(p2rels => !onboardedProductIds.contains(p2rels._1))
        .map(p2managers => {
          val productManagers = p2managers._2
          val lastManager     = retrieveLastRelationships(productManagers).head

          warnIfNoContract(lastManager)

          val tokenId = retrieveTokenId(Some(lastManager), productManagers)

          NewDesignInstitutionOnboarding(
            productId = lastManager.product.id,
            status = lastManager.state,
            tokenId = tokenId,
            contract = lastManager.filePath,
            pricingPlan = lastManager.pricingPlan,
            billing =
              lastManager.billing.get.copy(publicServices = lastManager.billing.get.publicServices.orElse(Some(false))),
            createdAt = productManagers.map(_.createdAt).min,
            updatedAt = lastManager.updatedAt
          )
        })

      newDesignInstitution = NewDesignInstitution(
        id = party.id.toString,
        externalId = party.externalId,
        origin = party.origin,
        originId = party.originId,
        description = party.description,
        institutionType = party.institutionType,
        digitalAddress = party.digitalAddress,
        address = party.address,
        zipCode = party.zipCode,
        taxCode = party.taxCode,
        geographicTaxonomies = party.geographicTaxonomies.map(PersistedGeographicTaxonomy.toApi),
        attributes = party.attributes.map(InstitutionAttribute.toApi).toSeq,
        paymentServiceProvider = party.paymentServiceProvider.map(PersistedPaymentServiceProvider.toAPi),
        dataProtectionOfficer = party.dataProtectionOfficer.map(PersistedDataProtectionOfficer.toApi),
        rea = party.rea,
        shareCapital = party.shareCapital,
        businessRegisterPlace = party.businessRegisterPlace,
        supportEmail = party.supportEmail,
        supportPhone = party.supportPhone,
        imported = party.imported.orElse(Some(false)),
        onboarding = onboardedProducts.concat(pendingProducts).toSeq,
        createdAt = party.start
      )
    } yield newDesignInstitution
  }

  /**
    * Code: 200, Message: collection of tokens modelled as new design, DataType: Seq[NewDesignToken]
    * Code: 500, Message: Error, DataType: Problem
    */
  override def findNewDesignTokens(tokenIds: Option[String], page: Int, size: Int)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerNewDesignInstitutionarray: ToEntityMarshaller[Seq[NewDesignToken]],
    contexts: Seq[(String, String)]
  ): Route = {
    val newDesignTokens: Future[Seq[NewDesignToken]] = for {
      tokens          <- tokenIds
        .map(s => retrieveTokens(parseArrayParameters(s)))
        .getOrElse(retrieveTokens(page, size))
      newDesignTokens <- Future.traverse(tokens)(token2NewDesignToken).map(_.flatten)
    } yield newDesignTokens

    onComplete(newDesignTokens) {
      case Success(result) =>
        findNewDesignTokens200(result)
      case Failure(ex)     =>
        logger.error(
          s"Exporting tokens using new design tokenIds=$tokenIds, page=$page, size=$size return an error",
          ex
        )
        val errorResponse: Problem =
          problemOf(StatusCodes.InternalServerError, FindNewDesignTokenError(ex.getMessage))
        findNewDesignTokens500(errorResponse)
    }
  }

  def retrieveTokens(tokenIds: List[String]): Future[Seq[Token]] =
    Future
      .traverse(tokenIds.map(_.toUUID).filter(_.isSuccess).map(_.get))(tokenIdUUID =>
        getCommander(tokenIdUUID.toString).ask(ref => GetToken(tokenIdUUID, ref))
      )
      .map(_.flatten)

  def retrieveTokens(page: Int, size: Int): Future[Seq[Token]] = {
    val commanders: List[EntityRef[Command]] = getAllCommanders

    for {
      results <- Future.traverse(commanders)(_.ask(ref => GetTokens(ref)))
      resultsFlatten = results.flatten
        .sortBy(_.id)
        .slice(page * size, page * size + size)
    } yield resultsFlatten
  }

  private def token2NewDesignToken(
    token: Token
  )(implicit contexts: Seq[(String, String)]): Future[Option[NewDesignToken]] = {
    val relationships = token.legals.map(_.relationshipId)

    for {
      legals <- Future
        .traverse(relationships)(relationshipService.getRelationshipById(_))
        .map(_.flatten)
      managers = legals.filter(t => t.role == PartyRole.MANAGER)
      manager  = managers.find(t => t.tokenId.isEmpty || t.tokenId.get == token.id)

      newDesignToken =
        if (managers.isEmpty) {
          logger.error(s"Found token without MANAGER: ${token.id} on users ${token.legals
              .map(_.partyId)} and relationshipIds $relationships valid until ${token.validity}")
          None
        } else {
          val managerNoTokenRelConfirmed = managers.head

          Some(
            NewDesignToken(
              id = token.id.toString,
              `type` = "INSTITUTION",
              status = manager.map(_.state).getOrElse(RelationshipState.PENDING),
              institutionId = managerNoTokenRelConfirmed.to.toString,
              productId = managerNoTokenRelConfirmed.product.id,
              expiringDate = token.validity,
              checksum = token.checksum,
              contractTemplate = token.contractInfo.path,
              contractVersion = token.contractInfo.version,
              contractSigned = manager.flatMap(_.filePath),
              users = legals.map(u => NewDesignTokenUser(userId = u.from.toString, role = u.role)),
              institutionUpdate = managerNoTokenRelConfirmed.institutionUpdate.map(i =>
                i.copy(imported = i.imported.orElse(Some(false)))
              ),
              createdAt = managerNoTokenRelConfirmed.createdAt,
              updatedAt = managerNoTokenRelConfirmed.updatedAt
            )
          )
        }
    } yield newDesignToken
  }
}
