package it.pagopa.interop.partymanagement.service.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.util.Timeout
import it.pagopa.interop.commons.utils.AkkaUtils
import it.pagopa.interop.partymanagement.model.party.{InstitutionParty, Party}
import it.pagopa.interop.partymanagement.model.persistence.{Command, GetInstitutionParties, GetParty, PartyPersistentBehavior}
import it.pagopa.interop.partymanagement.service._
import it.pagopa.interop.commons.utils.TypeConversions._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class InstitutionServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]]
)(implicit ec: ExecutionContext) extends InstitutionService {

  private val settings: ClusterShardingSettings = entity.settings.getOrElse(ClusterShardingSettings(system))

  private def getAllCommanders: List[EntityRef[Command]] = {
    val commanders = (0 until settings.numberOfShards)
      .map(shard => sharding.entityRefFor(PartyPersistentBehavior.TypeKey, shard.toString))
      .toList
    commanders
  }

  def getCommander(entityId: String): EntityRef[Command] =
    sharding.entityRefFor(PartyPersistentBehavior.TypeKey, AkkaUtils.getShard(entityId, settings.numberOfShards))

  override def getInstitutionById(institutionId: UUID)(implicit timeout: Timeout): Future[Option[Party]] = {
    getCommander(institutionId.toString).ask(ref => GetParty(institutionId, ref))
  }

  override def getInstitutionsByIds(ids: List[String])(implicit timeout: Timeout): Future[List[InstitutionParty]] =
    Future.traverse(ids)(id => id.toUUID.map(
      getInstitutionById(_)
        .map {
          case i: Option[InstitutionParty] => i
          case _ => None
        }
    ).getOrElse(Future.successful(None))).map(_.flatten)

  override def getInstitutions()(implicit timeout: Timeout): Future[Seq[InstitutionParty]] = {
    val commanders: List[EntityRef[Command]] = getAllCommanders

    for {
      results <- Future.traverse(commanders)(_.ask(ref => GetInstitutionParties(ref)))
      resultsFlatten = results.flatten
    } yield resultsFlatten
  }
}
