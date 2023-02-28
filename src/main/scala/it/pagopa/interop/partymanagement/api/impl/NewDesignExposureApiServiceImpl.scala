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
import scala.concurrent.{ExecutionContext, Future}
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

  private def getLastUpdatedRelationship: Seq[Relationship] => Relationship = rels =>
    rels.maxBy(r => r.updatedAt.getOrElse(r.createdAt))

  private def userRelationships2NewDesignUser: (UUID, Seq[Relationship]) => NewDesignUser = (uid, rels) =>
    NewDesignUser(
      id = uid.toString,
      createdAt = rels.map(_.createdAt).min,
      updatedAt = rels.map(_.updatedAt).max,
      bindings = rels
        .groupBy(_.to)
        .map(institutionUid2Rels =>
          NewDesignUserInstitution(
            institutionId = institutionUid2Rels._1.toString,
            createdAt = institutionUid2Rels._2.map(_.createdAt).min,
            products = institutionUid2Rels._2
              .groupBy(_.product.id)
              .map(productId2Rels => {
                val firstCreatedProductRole = productId2Rels._2.minBy(_.createdAt)
                val lastUpdatedProductRole  = getLastUpdatedRelationship(productId2Rels._2)

                val productRoles = retrieveLastRelationships(productId2Rels._2)

                val relsHavingContract    = productId2Rels._2.filter(_.filePath.nonEmpty)
                val lastRelHavingContract =
                  if (relsHavingContract.nonEmpty) Some(getLastUpdatedRelationship(relsHavingContract))
                  else Option.empty

                NewDesignUserInstitutionProduct(
                  productId = productId2Rels._1,
                  status = productRoles.head.state,
                  contract = lastRelHavingContract.flatMap(_.filePath),
                  role = lastUpdatedProductRole.role,
                  productRoles = productRoles.map(_.product.role),
                  env = "ROOT",
                  createdAt = firstCreatedProductRole.createdAt,
                  updatedAt = lastUpdatedProductRole.updatedAt
                )
              })
              .toSeq
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

  private def institutionParty2NewDesignInstitution: InstitutionParty => Future[NewDesignInstitution] = party => {

    def buildPremiumSubProduct(product2managers: Map[String, List[Relationship]], lastManager: Relationship) = {
      if (lastManager.product.id != "prod-io" || !product2managers.contains("prod-io-premium")) List.empty
      else {
        val subProductManagers    = product2managers("prod-io-premium")
        val subProductLastManager =
          retrieveLastRelationships(subProductManagers.filter(_.filePath.nonEmpty)).head

        List(
          NewDesignInstitutionOnboardingSubProduct(
            parentId = "prod-io",
            productId = "prod-io-premium",
            status = subProductLastManager.state,
            contract = subProductLastManager.filePath,
            pricingPlan = subProductLastManager.pricingPlan,
            createdAt = subProductManagers.map(_.createdAt).min,
            updatedAt = subProductLastManager.updatedAt
          )
        )
      }
    }

    for {
      product2managers <- relationshipService
        .retrieveRelationshipsByTo(party.id, List(PartyRole.MANAGER), List.empty, List.empty, List.empty)
        .map(_.groupBy(_.product.id))

      onboardedProducts = party.products
        .filter(p => p.product != "prod-io-premium")
        .map(p => {
          val productManagers = product2managers(p.product)
          val lastManager     = retrieveLastRelationships(productManagers.filter(_.filePath.nonEmpty)).head

          NewDesignInstitutionOnboarding(
            productId = p.product,
            status = lastManager.state,
            contract = lastManager.filePath,
            pricingPlan = p.pricingPlan,
            billing = p.billing.toBilling,
            subProducts = buildPremiumSubProduct(product2managers, lastManager),
            createdAt = productManagers.map(_.createdAt).min,
            updatedAt = lastManager.updatedAt
          )
        })

      onboardedProductIds = onboardedProducts.map(_.productId)

      pendingProducts = product2managers
        .filter(p2rels => !onboardedProductIds.contains(p2rels._1) && "prod-io-premium" != p2rels._1)
        .map(p2managers => {
          val productManagers = p2managers._2
          val lastManager     = retrieveLastRelationships(productManagers.filter(_.filePath.nonEmpty)).head

          NewDesignInstitutionOnboarding(
            productId = lastManager.product.id,
            status = lastManager.state,
            contract = lastManager.filePath,
            pricingPlan = lastManager.pricingPlan,
            billing = lastManager.billing.get,
            subProducts = buildPremiumSubProduct(product2managers, lastManager),
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
        imported = party.imported,
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
    for {
      legals <- Future
        .traverse(token.legals.map(_.relationshipId))(relationshipService.getRelationshipById(_))
        .map(_.flatten)
      manager = legals.find(_.role == PartyRole.MANAGER)

      newDesignToken =
        if (manager.isEmpty) {
          logger.error(s"Found token without MANAGER: ${token.id}")
          None
        } else
          Some(
            NewDesignToken(
              id = token.id.toString,
              status = manager.get.state,
              institutionId = token.legals.head.partyId.toString,
              productId = manager.get.product.id,
              expiringDate = token.validity,
              checksum = token.checksum,
              contractTemplate = token.contractInfo.path,
              contractVersion = token.contractInfo.version,
              contractSigned = manager.get.filePath,
              users = legals.map(_.from.toString),
              institutionUpdate = manager.get.institutionUpdate,
              createdAt = manager.get.createdAt,
              updatedAt = manager.get.updatedAt
            )
          )
    } yield newDesignToken
  }
}
