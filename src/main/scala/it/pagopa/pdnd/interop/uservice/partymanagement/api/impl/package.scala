package it.pagopa.pdnd.interop.uservice.partymanagement.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val personSeedFormat: RootJsonFormat[PersonSeed]               = jsonFormat3(PersonSeed)
  implicit val personFormat: RootJsonFormat[Person]                       = jsonFormat4(Person)
  implicit val organizationSeedFormat: RootJsonFormat[OrganizationSeed]   = jsonFormat4(OrganizationSeed)
  implicit val organizationFormat: RootJsonFormat[Organization]           = jsonFormat5(Organization)
  implicit val roleFormat: RootJsonFormat[Role]                           = jsonFormat1(Role)
  implicit val partyRelationShipFormat: RootJsonFormat[PartyRelationShip] = jsonFormat3(PartyRelationShip)
  implicit val problemFormat: RootJsonFormat[Problem]                     = jsonFormat3(Problem)
  implicit val tokenFeedFormat: RootJsonFormat[TokenSeed]                 = jsonFormat4(TokenSeed)
  implicit val tokenTextFormat: RootJsonFormat[TokenText]                 = jsonFormat1(TokenText)

}
