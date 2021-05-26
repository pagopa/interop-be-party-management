package it.pagopa.pdnd.interop.uservice.partymanagement.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val personSeedFormat: RootJsonFormat[PersonSeed]             = jsonFormat3(PersonSeed)
  implicit val personFormat: RootJsonFormat[Person]                     = jsonFormat4(Person)
  implicit val attributeRecordFormat: RootJsonFormat[AttributeRecord]   = jsonFormat2(AttributeRecord)
  implicit val organizationSeedFormat: RootJsonFormat[OrganizationSeed] = jsonFormat5(OrganizationSeed)
  implicit val organizationFormat: RootJsonFormat[Organization]         = jsonFormat6(Organization)
  implicit val roleFormat: RootJsonFormat[Role]                         = jsonFormat1(Role)
  implicit val relationShipSeedFormat: RootJsonFormat[RelationShipSeed] = jsonFormat3(RelationShipSeed)
  implicit val relationShipFormat: RootJsonFormat[RelationShip]         = jsonFormat4(RelationShip)
  implicit val relationShipsFormat: RootJsonFormat[RelationShips]       = jsonFormat1(RelationShips)
  implicit val problemFormat: RootJsonFormat[Problem]                   = jsonFormat3(Problem)
  implicit val tokenUserFormat: RootJsonFormat[TokenUser]               = jsonFormat3(TokenUser)
  implicit val tokenFeedFormat: RootJsonFormat[TokenSeed]               = jsonFormat2(TokenSeed)
  implicit val tokenTextFormat: RootJsonFormat[TokenText]               = jsonFormat1(TokenText)

}
