package it.pagopa.interop.partymanagement.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.interop.partymanagement.api.PartyApiMarshaller
import it.pagopa.interop.partymanagement.model._
import spray.json._

object PartyApiMarshallerImpl extends PartyApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def toEntityMarshallerPerson: ToEntityMarshaller[Person] = sprayJsonMarshaller[Person]

  override implicit def toEntityMarshallerInstitution: ToEntityMarshaller[Institution] =
    sprayJsonMarshaller[Institution]

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] =
    sprayJsonMarshaller[Problem]

  override implicit def fromEntityUnmarshallerPersonSeed: FromEntityUnmarshaller[PersonSeed] =
    sprayJsonUnmarshaller[PersonSeed]

  override implicit def fromEntityUnmarshallerInstitutionSeed: FromEntityUnmarshaller[InstitutionSeed] =
    sprayJsonUnmarshaller[InstitutionSeed]

  override implicit def fromEntityUnmarshallerInstitution: FromEntityUnmarshaller[Institution] =
    sprayJsonUnmarshaller[Institution]

  override implicit def toEntityMarshallerRelationships: ToEntityMarshaller[Relationships] =
    sprayJsonMarshaller[Relationships]

  override implicit def fromEntityUnmarshallerTokenSeed: FromEntityUnmarshaller[TokenSeed] =
    sprayJsonUnmarshaller[TokenSeed]

  override implicit def toEntityMarshallerTokenText: ToEntityMarshaller[TokenText] = sprayJsonMarshaller[TokenText]

  override implicit def fromEntityUnmarshallerRelationshipSeed: FromEntityUnmarshaller[RelationshipSeed] =
    sprayJsonUnmarshaller[RelationshipSeed]

  override implicit def toEntityMarshallerRelationship: ToEntityMarshaller[Relationship] =
    sprayJsonMarshaller[Relationship]

  override implicit def toEntityMarshallerBulkInstitutions: ToEntityMarshaller[BulkInstitutions] =
    sprayJsonMarshaller[BulkInstitutions]

  override implicit def fromEntityUnmarshallerBulkPartiesSeed: FromEntityUnmarshaller[BulkPartiesSeed] =
    sprayJsonUnmarshaller[BulkPartiesSeed]

  override implicit def fromEntityUnmarshallerAttributeList: FromEntityUnmarshaller[Seq[Attribute]] =
    sprayJsonUnmarshaller[Seq[Attribute]]

  override implicit def toEntityMarshallerAttributearray: ToEntityMarshaller[Seq[Attribute]] =
    sprayJsonMarshaller[Seq[Attribute]]
}
