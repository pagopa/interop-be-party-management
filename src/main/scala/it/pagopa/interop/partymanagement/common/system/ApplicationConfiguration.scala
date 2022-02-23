package it.pagopa.interop.partymanagement.common.system

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.ListHasAsScala

object ApplicationConfiguration {
  lazy val config: Config = ConfigFactory.load()

  lazy val serverPort: Int = config.getInt("party-management.port")

  lazy val tokenValidityHours: Long = config.getLong("party-management.token-validity-hours")

  lazy val jwtAudience: Set[String] = config.getStringList("party-management.jwt.audience").asScala.toSet

  lazy val storageContainer: String = config.getString("party-management.storage.container")

  lazy val contractPath: String = config.getString("party-management.storage.contract-path")

}
