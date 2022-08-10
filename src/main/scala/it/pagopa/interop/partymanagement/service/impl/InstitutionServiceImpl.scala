package it.pagopa.interop.partymanagement.service.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.util.Timeout
import it.pagopa.interop.commons.utils.AkkaUtils
import it.pagopa.interop.partymanagement.model.party.Party
import it.pagopa.interop.partymanagement.model.persistence.{Command, GetParty, PartyPersistentBehavior}
import it.pagopa.interop.partymanagement.service._

import java.util.UUID
import scala.concurrent.Future

class InstitutionServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]]
) extends InstitutionService {

  private val settings: ClusterShardingSettings = entity.settings match {
    case None    => ClusterShardingSettings(system)
    case Some(s) => s
  }

  def getCommander(entityId: String): EntityRef[Command] =
    sharding.entityRefFor(PartyPersistentBehavior.TypeKey, AkkaUtils.getShard(entityId, settings.numberOfShards))

  override def getInstitutionById(institutionId: UUID)(implicit timeout: Timeout): Future[Option[Party]] = {
    getCommander(institutionId.toString).ask(ref => GetParty(institutionId, ref))
  }

}
