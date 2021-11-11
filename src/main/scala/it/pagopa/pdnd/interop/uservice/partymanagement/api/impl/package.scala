package it.pagopa.pdnd.interop.uservice.partymanagement.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}

import java.util.UUID
import scala.util.{Failure, Success, Try}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val uuidFormat: JsonFormat[UUID] =
    new JsonFormat[UUID] {
      override def write(obj: UUID): JsValue = JsString(obj.toString)

      override def read(json: JsValue): UUID = json match {
        case JsString(s) =>
          Try(UUID.fromString(s)) match {
            case Success(result) => result
            case Failure(exception) =>
              deserializationError(s"could not parse $s as UUID", exception)
          }
        case notAJsString =>
          deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
      }
    }

  implicit val personSeedFormat: RootJsonFormat[PersonSeed]               = jsonFormat1(PersonSeed)
  implicit val personFormat: RootJsonFormat[Person]                       = jsonFormat1(Person)
  implicit val organizationSeedFormat: RootJsonFormat[OrganizationSeed]   = jsonFormat8(OrganizationSeed)
  implicit val organizationFormat: RootJsonFormat[Organization]           = jsonFormat9(Organization)
  implicit val relationshipFormat: RootJsonFormat[Relationship]           = jsonFormat10(Relationship)
  implicit val relationshipSeedFormat: RootJsonFormat[RelationshipSeed]   = jsonFormat5(RelationshipSeed)
  implicit val relationshipsFormat: RootJsonFormat[Relationships]         = jsonFormat1(Relationships)
  implicit val relationshipsSeedFormat: RootJsonFormat[RelationshipsSeed] = jsonFormat1(RelationshipsSeed)
  implicit val problemFormat: RootJsonFormat[Problem]                     = jsonFormat3(Problem)
  implicit val tokenFeedFormat: RootJsonFormat[TokenSeed]                 = jsonFormat3(TokenSeed)
  implicit val tokenTextFormat: RootJsonFormat[TokenText]                 = jsonFormat1(TokenText)
  implicit val bulkPartiesSeedFormat: RootJsonFormat[BulkPartiesSeed]     = jsonFormat1(BulkPartiesSeed)
  implicit val bulkOrganizationsFormat: RootJsonFormat[BulkOrganizations] = jsonFormat2(BulkOrganizations)
  implicit val productsFormat: RootJsonFormat[Products]                   = jsonFormat1(Products)
}
