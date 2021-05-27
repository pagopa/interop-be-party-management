package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PartyApiMarshaller
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import spray.json._

class PartyApiMarshallerImpl extends PartyApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def fromEntityUnmarshallerRelationShip: FromEntityUnmarshaller[RelationShip] =
    sprayJsonUnmarshaller[RelationShip]

  override implicit def fromEntityUnmarshallerAttributeRecordList: FromEntityUnmarshaller[Seq[AttributeRecord]] =
    sprayJsonUnmarshaller[Seq[AttributeRecord]]

  override implicit def toEntityMarshallerPerson: ToEntityMarshaller[Person] = sprayJsonMarshaller[Person]

  implicit def fromEntityUnmarshallerPerson: FromEntityUnmarshaller[Person] = sprayJsonUnmarshaller[Person]

  override implicit def toEntityMarshallerOrganization: ToEntityMarshaller[Organization] =
    sprayJsonMarshaller[Organization]

  implicit def fromEntityUnmarshallerOrganization: FromEntityUnmarshaller[Organization] =
    sprayJsonUnmarshaller[Organization]

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] =
    sprayJsonMarshaller[Problem]

  override implicit def fromEntityUnmarshallerPersonSeed: FromEntityUnmarshaller[PersonSeed] =
    sprayJsonUnmarshaller[PersonSeed]

  override implicit def fromEntityUnmarshallerOrganizationSeed: FromEntityUnmarshaller[OrganizationSeed] =
    sprayJsonUnmarshaller[OrganizationSeed]

  override implicit def toEntityMarshallerRelationShips: ToEntityMarshaller[RelationShips] =
    sprayJsonMarshaller[RelationShips]

  implicit def fromEntityMarshallerRelationShips: FromEntityUnmarshaller[RelationShips] =
    sprayJsonUnmarshaller[RelationShips]

  override implicit def fromEntityUnmarshallerTokenSeed: FromEntityUnmarshaller[TokenSeed] =
    sprayJsonUnmarshaller[TokenSeed]

  override implicit def toEntityMarshallerTokenText: ToEntityMarshaller[TokenText] = sprayJsonMarshaller[TokenText]
}
