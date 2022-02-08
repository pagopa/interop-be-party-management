package it.pagopa.pdnd.interop.uservice.partymanagement.common.system

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.ListHasAsScala

object ApplicationConfiguration {
  lazy val config: Config = ConfigFactory.load()

  lazy val  serverPort: Int = config.getInt("uservice-party-management.port")

  lazy val  tokenValidityHours: Long = config.getLong("uservice-party-management.token-validity-hours")

  lazy val  jwtAudience: Set[String] = config.getStringList("uservice-party-management.jwt.audience").asScala.toSet

  lazy val  storageContainer: String = config.getString("uservice-party-management.storage.container")

  lazy val  contractPath: String = config.getString("uservice-party-management.storage.contract-path")

}
