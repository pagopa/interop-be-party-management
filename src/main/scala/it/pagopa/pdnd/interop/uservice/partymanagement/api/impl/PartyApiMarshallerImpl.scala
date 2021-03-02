package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PartyApiMarshaller
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{ErrorResponse, Institution, PartyRelationShip, Person}
import spray.json._

class PartyApiMarshallerImpl extends PartyApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def toEntityMarshallerPartyRelationShip: ToEntityMarshaller[PartyRelationShip] =
    sprayJsonMarshaller[PartyRelationShip]

  override implicit def toEntityMarshallerInstitution: ToEntityMarshaller[Institution] =
    sprayJsonMarshaller[Institution]

  override implicit def fromEntityUnmarshallerInstitution: FromEntityUnmarshaller[Institution] =
    sprayJsonUnmarshaller[Institution]

  override implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] = errorResponseMarshaller

  override implicit def toEntityMarshallerPerson: ToEntityMarshaller[Person] = sprayJsonMarshaller[Person]

  override implicit def fromEntityUnmarshallerPerson: FromEntityUnmarshaller[Person] = sprayJsonUnmarshaller[Person]
}
