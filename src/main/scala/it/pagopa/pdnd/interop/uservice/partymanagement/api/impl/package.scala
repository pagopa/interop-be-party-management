package it.pagopa.pdnd.interop.uservice.partymanagement.api

import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.util.Timeout
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.Party
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.{Command, GetParty}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val personSeedFormat: RootJsonFormat[PersonSeed]             = jsonFormat3(PersonSeed)
  implicit val personFormat: RootJsonFormat[Person]                     = jsonFormat4(Person)
  implicit val attributeRecordFormat: RootJsonFormat[AttributeRecord]   = jsonFormat2(AttributeRecord)
  implicit val organizationSeedFormat: RootJsonFormat[OrganizationSeed] = jsonFormat5(OrganizationSeed)
  implicit val organizationFormat: RootJsonFormat[Organization]         = jsonFormat6(Organization)
  implicit val relationShipFormat: RootJsonFormat[RelationShip]         = jsonFormat4(RelationShip)
  implicit val relationShipsFormat: RootJsonFormat[RelationShips]       = jsonFormat1(RelationShips)
  implicit val problemFormat: RootJsonFormat[Problem]                   = jsonFormat3(Problem)
  implicit val tokenFeedFormat: RootJsonFormat[TokenSeed]               = jsonFormat3(TokenSeed)
  implicit val tokenTextFormat: RootJsonFormat[TokenText]               = jsonFormat1(TokenText)

  implicit class CommandersOps(val commanders: Seq[EntityRef[Command]]) extends AnyVal {
    def getParty(id: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Option[Party]] = {
      Future
        .sequence(commanders.map(commander => commander.ask(ref => GetParty(id, ref))))
        .map(_.flatten.headOption)
    }
  }

}
