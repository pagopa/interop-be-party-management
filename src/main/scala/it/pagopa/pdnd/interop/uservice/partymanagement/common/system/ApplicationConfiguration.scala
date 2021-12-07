package it.pagopa.pdnd.interop.uservice.partymanagement.common.system

import com.typesafe.config.{Config, ConfigFactory}

case class StorageAccountInfo(applicationId: String, applicationSecret: String, endpoint: String, container: String)

object ApplicationConfiguration {
  lazy val config: Config = ConfigFactory.load()

  def serverPort: Int = {
    config.getInt("uservice-party-management.port")
  }

  def runtimeFileManager: String = {
    config.getString("uservice-party-management.storage.type")
  }

  def tokenValidityHours: Long = {
    config.getLong("uservice-party-management.token-validity-hours")
  }

  def storageContainer: String = config.getString("uservice-party-management.storage.container")
}
