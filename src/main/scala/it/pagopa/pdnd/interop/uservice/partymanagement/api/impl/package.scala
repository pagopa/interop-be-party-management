package it.pagopa.pdnd.interop.uservice.partymanagement.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.commons.utils.SprayCommonFormats.uuidFormat
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val personSeedFormat: RootJsonFormat[PersonSeed]               = jsonFormat1(PersonSeed)
  implicit val personFormat: RootJsonFormat[Person]                       = jsonFormat1(Person)
  implicit val organizationSeedFormat: RootJsonFormat[OrganizationSeed]   = jsonFormat6(OrganizationSeed)
  implicit val organizationFormat: RootJsonFormat[Organization]           = jsonFormat7(Organization)
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
