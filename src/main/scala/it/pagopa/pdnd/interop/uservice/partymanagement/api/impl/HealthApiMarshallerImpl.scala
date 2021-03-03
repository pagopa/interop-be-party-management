package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.pdnd.interop.uservice.partymanagement.api.HealthApiMarshaller
import it.pagopa.pdnd.interop.uservice.partymanagement.model.ErrorResponse

class HealthApiMarshallerImpl extends HealthApiMarshaller {

  override implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] =
    sprayJsonMarshaller[ErrorResponse](jsonFormat3(ErrorResponse))
}
