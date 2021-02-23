package it.pagopa.pdnd.interop.uservice.partymanagement.server.impl

import org.slf4j.LoggerFactory

import scala.concurrent.Future

// Enabled in application.conf
class HealthCheck() extends (() => Future[Boolean]) {

  private val log = LoggerFactory.getLogger(getClass)

  override def apply(): Future[Boolean] = {
    log.info("HealthCheck called")
    Future.successful(true)
  }
}

class LiveCheck() extends (() => Future[Boolean]) {

  private val log = LoggerFactory.getLogger(getClass)

  override def apply(): Future[Boolean] = {
    log.info("LiveCheck called")
    Future.successful(true)
  }
}
