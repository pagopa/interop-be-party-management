package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partymanagement.api.HealthApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.model.Problem

class HealthServiceApiImpl extends HealthApiService {

  override def getStatus()(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val response: Problem = Problem(
      `type` = "about:blank",
      status = StatusCodes.OK.intValue,
      title = StatusCodes.OK.defaultMessage,
      errors = Seq.empty
    )
    getStatus200(response)
  }
}
