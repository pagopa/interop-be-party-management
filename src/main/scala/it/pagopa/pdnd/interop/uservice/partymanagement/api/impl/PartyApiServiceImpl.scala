package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PartyApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Credential, ErrorResponse, Person}

class PartyApiServiceImpl extends PartyApiService {
  override def createPerson(person: Person)(implicit toEntityMarshallerPerson: ToEntityMarshaller[Person]): Route = {
    createPerson200(person)
  }

  override def getPersonByFiscalCode(fiscalCode: String)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getCredentialById(credentialId: String)(implicit
    toEntityMarshallerCredential: ToEntityMarshaller[Credential],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???
}
