package it.pagopa.interop.partymanagement.service.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.util.Timeout
import it.pagopa.interop.partymanagement.model.Relationship
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
