package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.pdnd.interop.uservice.partymanagement.api.HealthApiMarshaller
import it.pagopa.pdnd.interop.uservice.partymanagement.model.Problem

object HealthApiMarshallerImpl extends HealthApiMarshaller {

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] =
    sprayJsonMarshaller[Problem]
}
