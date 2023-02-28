package it.pagopa.interop.partymanagement.service.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.util.Timeout
import it.pagopa.interop.commons.utils.AkkaUtils
import it.pagopa.interop.partymanagement.error.PartyManagementErrors.MissingQueryParam
import it.pagopa.interop.partymanagement.model.party.PersistedPartyRelationship
import it.pagopa.interop.partymanagement.model.{PartyRole, Relationship, RelationshipState}
import it.pagopa.interop.partymanagement.model.persistence._
import it.pagopa.interop.partymanagement.service._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class RelationshipServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]]
)(implicit ec: ExecutionContext)
    extends RelationshipService {

  private val settings: ClusterShardingSettings = entity.settings.getOrElse(ClusterShardingSettings(system))

  private def getAllCommanders: List[EntityRef[Command]] = {
    val commanders = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList
    commanders
  }

  private def getCommander(entityId: String): EntityRef[Command] =
    sharding.entityRefFor(PartyPersistentBehavior.TypeKey, AkkaUtils.getShard(entityId, settings.numberOfShards))

  override def getRelationshipById(relationshipId: UUID)(implicit timeout: Timeout): Future[Option[Relationship]] = {
    val commanders = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    val result: Future[Option[Relationship]] =
      for {
        results <- Future.traverse(commanders)(_.ask(ref => GetPartyRelationshipById(relationshipId, ref)))
        maybePartyRelationship = results.find(_.isDefined).flatten
        partyRelationship      = maybePartyRelationship.map(_.toRelationship)
      } yield partyRelationship
    result
  }

  override def retrieveRelationshipsByTo(
    id: UUID,
    roles: List[PartyRole],
    states: List[RelationshipState],
    product: List[String],
    productRoles: List[String]
  )(implicit timeout: Timeout): Future[List[Relationship]] = {
    val commanders: List[EntityRef[Command]] = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList

    for {
      re <- Future.traverse(commanders)(
        _.ask[List[PersistedPartyRelationship]](ref =>
          GetPartyRelationshipsByTo(id, roles, states, product, productRoles, ref)
        )
      )
    } yield re.flatten.map(_.toRelationship)
  }

  override def retrieveRelationshipsByFrom(
    id: UUID,
    roles: List[PartyRole],
    states: List[RelationshipState],
    product: List[String],
    productRoles: List[String]
  )(implicit timeout: Timeout): Future[List[Relationship]] =
    for {
      re <- getCommander(id.toString).ask(ref =>
        GetPartyRelationshipsByFrom(id, roles, states, product, productRoles, ref)
      )
    } yield re.map(_.toRelationship)

  override def relationshipsFromParams(
    from: Option[UUID],
    to: Option[UUID],
    roles: List[PartyRole],
    states: List[RelationshipState],
    product: List[String],
    productRoles: List[String]
  )(implicit timeout: Timeout): Future[List[Relationship]] = (from, to) match {
    case (Some(f), Some(t)) =>
      retrieveRelationshipsByFrom(f, roles, states, product, productRoles).map(_.filter(_.to == t))
    case (Some(f), None)    => retrieveRelationshipsByFrom(f, roles, states, product, productRoles)
    case (None, Some(t))    => retrieveRelationshipsByTo(t, roles, states, product, productRoles)
    case _                  => Future.failed(MissingQueryParam)
  }

  override def getRelationshipsByUserIds(userIds: List[UUID])(implicit timeout: Timeout): Future[Seq[Relationship]] = {
    val commanders: List[EntityRef[Command]] = getAllCommanders

    for {
      results <- Future.traverse(commanders)(_.ask(ref => GetPartyRelationshipsByUserIds(userIds, List.empty, ref)))
      resultsFlatten    = results.flatten
      partyRelationship = resultsFlatten.map(_.toRelationship)
    } yield partyRelationship
  }

  override def getRelationships()(implicit timeout: Timeout): Future[Seq[Relationship]] = {
    val commanders: List[EntityRef[Command]] = getAllCommanders

    for {
      results <- Future.traverse(commanders)(_.ask(ref => GetPartyRelationships(List.empty, ref)))
      resultsFlatten    = results.flatten
      partyRelationship = resultsFlatten.map(_.toRelationship)
    } yield partyRelationship
  }
}
