package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.Logger
import it.pagopa.pdnd.interop.commons.utils.CorrelationId
import it.pagopa.pdnd.interop.uservice.partymanagement.api.HealthApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.model.Problem
import org.slf4j.LoggerFactory
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.CanLogCorrelationId

class HealthServiceApiImpl extends HealthApiService {

  val logger = Logger.takingImplicit[CorrelationId](LoggerFactory.getLogger(this.getClass))

  override def getStatus()(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)],
    correlationId: CorrelationId
  ): Route = {
    logger.info("hello")
    val response: Problem = Problem(
      `type` = "about:blank",
      status = StatusCodes.OK.intValue,
      title = StatusCodes.OK.defaultMessage,
      errors = Seq.empty
    )
    getStatus200(response)
  }
}
