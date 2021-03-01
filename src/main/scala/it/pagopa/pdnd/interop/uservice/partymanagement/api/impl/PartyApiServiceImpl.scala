package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PartyApiService
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Credential, ErrorResponse, Person}

class PartyApiServiceImpl extends PartyApiService {
  override def createPerson200(responsePerson: Person)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person]
  ): Route = ???

  override def getCredentialById200(responseCredential: Credential)(implicit
    toEntityMarshallerCredential: ToEntityMarshaller[Credential]
  ): Route = super.getCredentialById200(responseCredential)

  override def getCredentialById400(responseErrorResponse: ErrorResponse)(implicit
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getCredentialById404(responseErrorResponse: ErrorResponse)(implicit
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getPersonByFiscalCode200(responsePerson: Person)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person]
  ): Route = ???

  override def getPersonByFiscalCode400(responseErrorResponse: ErrorResponse)(implicit
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getPersonByFiscalCode404(responseErrorResponse: ErrorResponse)(implicit
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def createPerson(person: Person)(implicit toEntityMarshallerPerson: ToEntityMarshaller[Person]): Route = ???

  override def getPersonByFiscalCode(fiscalCode: String)(implicit
    toEntityMarshallerPerson: ToEntityMarshaller[Person],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getCredentialById(credentialId: String)(implicit
    toEntityMarshallerCredential: ToEntityMarshaller[Credential],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???
}
