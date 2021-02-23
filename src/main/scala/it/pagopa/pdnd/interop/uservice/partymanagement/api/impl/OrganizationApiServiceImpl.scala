package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partymanagement.api.OrganizationApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Organization, OrganizationError}

class OrganizationApiServiceImpl extends OrganizationApiService {
  override def getOrganizationById200(responseOrganization: Organization)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization]
  ): Route = ???

  override def getOrganizationById400(responseError: OrganizationError)(implicit
    toEntityMarshallerError: ToEntityMarshaller[OrganizationError]
  ): Route = ???

  override def getOrganizationById404(responseError: OrganizationError)(implicit
    toEntityMarshallerError: ToEntityMarshaller[OrganizationError]
  ): Route = ???

  override def getOrganizationById(orgId: String)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerError: ToEntityMarshaller[OrganizationError]
  ): Route = ???

}
