package it.pagopa.interop.partymanagement.common.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {
  val config: Config = ConfigFactory.load()

  val serverPort: Int = config.getInt("party-management.port")

  val tokenValidityHours: Long = config.getLong("party-management.token-validity-hours")

  val jwtAudience: Set[String] =
    config.getString("party-management.jwt.audience").split(",").toSet.filter(_.nonEmpty)

  val storageContainer: String = config.getString("party-management.storage.container")

  val contractPath: String = config.getString("party-management.storage.contract-path")

  val numberOfProjectionTags: Int = config.getInt("akka.cluster.sharding.number-of-shards")
  def projectionTag(index: Int)   = s"interop-be-party-management-persistence|$index"
  val projectionsEnabled: Boolean = config.getBoolean("akka.projection.enabled")
}
