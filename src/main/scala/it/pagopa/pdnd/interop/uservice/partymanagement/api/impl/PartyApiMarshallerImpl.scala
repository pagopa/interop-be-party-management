package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PartyApiMarshaller
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import spray.json._

object PartyApiMarshallerImpl extends PartyApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def toEntityMarshallerPerson: ToEntityMarshaller[Person] = sprayJsonMarshaller[Person]

  override implicit def toEntityMarshallerOrganization: ToEntityMarshaller[Organization] =
    sprayJsonMarshaller[Organization]

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] =
    sprayJsonMarshaller[Problem]

  override implicit def fromEntityUnmarshallerPersonSeed: FromEntityUnmarshaller[PersonSeed] =
    sprayJsonUnmarshaller[PersonSeed]

  override implicit def fromEntityUnmarshallerOrganizationSeed: FromEntityUnmarshaller[OrganizationSeed] =
    sprayJsonUnmarshaller[OrganizationSeed]

  override implicit def toEntityMarshallerRelationships: ToEntityMarshaller[Relationships] =
    sprayJsonMarshaller[Relationships]

  override implicit def fromEntityUnmarshallerTokenSeed: FromEntityUnmarshaller[TokenSeed] =
    sprayJsonUnmarshaller[TokenSeed]

  override implicit def toEntityMarshallerTokenText: ToEntityMarshaller[TokenText] = sprayJsonMarshaller[TokenText]

  override implicit def fromEntityUnmarshallerRelationshipSeed: FromEntityUnmarshaller[RelationshipSeed] =
    sprayJsonUnmarshaller[RelationshipSeed]

  override implicit def toEntityMarshallerRelationship: ToEntityMarshaller[Relationship] =
    sprayJsonMarshaller[Relationship]

  override implicit def toEntityMarshallerBulkOrganizations: ToEntityMarshaller[BulkOrganizations] =
    sprayJsonMarshaller[BulkOrganizations]

  override implicit def fromEntityUnmarshallerBulkPartiesSeed: FromEntityUnmarshaller[BulkPartiesSeed] =
    sprayJsonUnmarshaller[BulkPartiesSeed]

  override implicit def toEntityMarshallerTokenInfo: ToEntityMarshaller[TokenInfo] = sprayJsonMarshaller[TokenInfo]

  override implicit def fromEntityUnmarshallerAttributeList: FromEntityUnmarshaller[Seq[Attribute]] =
    sprayJsonUnmarshaller[Seq[Attribute]]

  override implicit def toEntityMarshallerAttributearray: ToEntityMarshaller[Seq[Attribute]] =
    sprayJsonMarshaller[Seq[Attribute]]
}
