package it.pagopa.pdnd.interop.uservice.partymanagement.common.system

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.ListHasAsScala

object ApplicationConfiguration {
  lazy val config: Config = ConfigFactory.load()

  def serverPort: Int = config.getInt("uservice-party-management.port")

  def tokenValidityHours: Long = config.getLong("uservice-party-management.token-validity-hours")

  def jwtAudience: Set[String] = config.getStringList("uservice-party-management.jwt.audience").asScala.toSet

  def storageContainer: String = config.getString("uservice-party-management.storage.container")

  def contractPath: String = config.getString("uservice-party-management.storage.contract-path")

}
