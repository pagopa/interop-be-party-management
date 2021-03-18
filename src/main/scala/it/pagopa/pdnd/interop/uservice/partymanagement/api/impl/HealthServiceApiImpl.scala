package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partymanagement.api.HealthApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.model.Problem

class HealthServiceApiImpl extends HealthApiService {

  override def getStatus()(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route = getStatus200(
    Problem(None, 200, "OK")
  )

}