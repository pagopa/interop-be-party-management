package it.pagopa.interop.partymanagement.api.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils
import it.pagopa.interop.commons.utils.OpenapiUtils.parseArrayParameters
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.partymanagement.api.NewDesignExposureApiService
import it.pagopa.interop.partymanagement.common.system._
import it.pagopa.interop.partymanagement.error.PartyManagementErrors.FindNewDesignUserError
import it.pagopa.interop.partymanagement.model._
import it.pagopa.interop.partymanagement.model.persistence._
import it.pagopa.interop.partymanagement.service.RelationshipService
import org.slf4j.LoggerFactory

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class NewDesignExposureApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]],
  relationshipService: RelationshipService
)(implicit ec: ExecutionContext)
    extends NewDesignExposureApiService {

  val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](LoggerFactory.getLogger(this.getClass))

  private val settings: ClusterShardingSettings = entity.settings match {
    case None    => ClusterShardingSettings(system)
    case Some(s) => s
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
        logger.error(s"Exporting users using new design userIds=$userIds, page=$page, size=$size return an error", ex)
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

                val productRoles                                = retrieveLastProductRoles(
                  productId2Rels._2,
                  List(
                    RelationshipState.ACTIVE,
                    RelationshipState.SUSPENDED,
                    RelationshipState.TOBEVALIDATED,
                    RelationshipState.PENDING,
                    RelationshipState.REJECTED,
                    RelationshipState.DELETED
                  )
                )
                val relsHavingContract    = productId2Rels._2.filter(_.filePath.nonEmpty)
                val lastRelHavingContract = if (relsHavingContract.nonEmpty) Some(getLastUpdatedRelationship(relsHavingContract)) else Option.empty

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

  def retrieveLastProductRoles: (Seq[Relationship], List[RelationshipState]) => Seq[Relationship] =
    (productRels, stateOrder) => {
      val relsHavingHeadState = productRels.filter(_.state == stateOrder.head)

      if (relsHavingHeadState.nonEmpty) {
        relsHavingHeadState
      } else {
        retrieveLastProductRoles(productRels, stateOrder.slice(1, stateOrder.size))
      }
    }
}
