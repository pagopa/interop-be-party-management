package it.pagopa.interop.partymanagement.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.interop.partymanagement.api.PublicApiMarshaller
import it.pagopa.interop.partymanagement.model._
import spray.json._

object PublicApiMarshallerImpl extends PublicApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] =
    sprayJsonMarshaller[Problem]

  override implicit def toEntityMarshallerTokenInfo: ToEntityMarshaller[TokenInfo] = sprayJsonMarshaller[TokenInfo]

}
