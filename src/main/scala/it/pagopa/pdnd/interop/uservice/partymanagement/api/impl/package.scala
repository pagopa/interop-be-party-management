package it.pagopa.pdnd.interop.uservice.partymanagement.api

import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.util.Timeout
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{Party, PartyRelationShip}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.{Command, GetParty, GetPartyRelationShipsByTo}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val personSeedFormat: RootJsonFormat[PersonSeed]             = jsonFormat3(PersonSeed)
  implicit val personFormat: RootJsonFormat[Person]                     = jsonFormat4(Person)
  implicit val organizationSeedFormat: RootJsonFormat[OrganizationSeed] = jsonFormat6(OrganizationSeed)
  implicit val organizationFormat: RootJsonFormat[Organization]         = jsonFormat7(Organization)
  implicit val relationShipFormat: RootJsonFormat[RelationShip]         = jsonFormat4(RelationShip)
  implicit val relationShipsFormat: RootJsonFormat[RelationShips]       = jsonFormat1(RelationShips)
  implicit val problemFormat: RootJsonFormat[Problem]                   = jsonFormat3(Problem)
  implicit val tokenFeedFormat: RootJsonFormat[TokenSeed]               = jsonFormat3(TokenSeed)
  implicit val tokenTextFormat: RootJsonFormat[TokenText]               = jsonFormat1(TokenText)

  implicit class CommandersOps(val commanders: List[EntityRef[Command]]) extends AnyVal {

    def extractRelationships(
      partyRelationShips: List[PartyRelationShip]
    )(implicit ec: ExecutionContext, timeout: Timeout): Future[List[RelationShip]] =
      Future.traverse(partyRelationShips)(commanders.extractRelationship).map(_.flatten)

    def extractRelationship(
      partyRelationShip: PartyRelationShip
    )(implicit ec: ExecutionContext, timeout: Timeout): Future[Option[RelationShip]] = {
      val partiesExtracted: Future[List[(Option[Party], Option[Party])]] = Future.traverse(commanders) { commander =>
        for {
          from <- commander.ask(ref => GetParty(partyRelationShip.id.from, ref))
          to   <- commander.ask(ref => GetParty(partyRelationShip.id.to, ref))
        } yield (from, to)

      }

      partiesExtracted.map { parties =>
        for {
          from <- parties.find(_._1.isDefined).flatMap(_._1)
          to   <- parties.find(_._2.isDefined).flatMap(_._2)
        } yield RelationShip(
          from = from.externalId,
          to = to.externalId,
          role = partyRelationShip.id.role.stringify,
          status = Some(partyRelationShip.status.stringify)
        )
      }

    }

    def askForPartyRelationShips(
      id: UUID
    )(implicit ec: ExecutionContext, timeout: Timeout): Future[List[PartyRelationShip]] =
      Future.traverse(commanders)(commander => commander.ask(ref => GetPartyRelationShipsByTo(id, ref))).map(_.flatten)
  }

}
