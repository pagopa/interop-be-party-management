package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partymanagement.api.HealthApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.model.ErrorResponse

class HealthServiceApiImpl extends HealthApiService {

  override def getStatus200(responseProblem: ErrorResponse)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getStatus()(implicit toEntityMarshallerProblem: ToEntityMarshaller[ErrorResponse]): Route = ???

}
