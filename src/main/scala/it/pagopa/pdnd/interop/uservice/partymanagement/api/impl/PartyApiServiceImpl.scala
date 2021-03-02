package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PartyApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{ErrorResponse, Institution, PartyRelationShip, Person}

class PartyApiServiceImpl extends PartyApiService {
  override def createPerson(person: Person)(implicit toEntityMarshallerPerson: ToEntityMarshaller[Person]): Route = {
    createPerson200(person)
  }

  override def createRelationShip(taxCode: String, institutionId: String)(implicit
    toEntityMarshallerPartyRelationShip: ToEntityMarshaller[PartyRelationShip]
  ): Route = ???

  override def getInstitutionByID(institutionId: String)(implicit
    toEntityMarshallerInstitution: ToEntityMarshaller[Institution],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getPersonByTaxCode(taxCode: String)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???
}
