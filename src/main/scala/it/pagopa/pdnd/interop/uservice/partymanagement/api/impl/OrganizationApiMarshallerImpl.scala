package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.pdnd.interop.uservice.partymanagement.api.OrganizationApiMarshaller
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{OrganizationError, Organization}
import spray.json._

class OrganizationApiMarshallerImpl extends OrganizationApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {
  override implicit def toEntityMarshallerOrganization: ToEntityMarshaller[Organization] = sprayJsonMarshaller(
    jsonFormat4(Organization.apply)
  )

  override implicit def toEntityMarshallerOrganizationError: ToEntityMarshaller[OrganizationError] =
    sprayJsonMarshaller(jsonFormat4(OrganizationError.apply))
}
