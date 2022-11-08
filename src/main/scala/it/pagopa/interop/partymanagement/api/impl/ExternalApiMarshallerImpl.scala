package it.pagopa.interop.partymanagement.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.interop.partymanagement.api.ExternalApiMarshaller
import it.pagopa.interop.partymanagement.model._
import spray.json._

object ExternalApiMarshallerImpl extends ExternalApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def toEntityMarshallerInstitution: ToEntityMarshaller[Institution] =
    sprayJsonMarshaller[Institution]

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] =
    sprayJsonMarshaller[Problem]

  override implicit def toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions] =
    sprayJsonMarshaller[Institutions]
}
