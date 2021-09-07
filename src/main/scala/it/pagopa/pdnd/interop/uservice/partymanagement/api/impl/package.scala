package it.pagopa.pdnd.interop.uservice.partymanagement.api

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.util.Timeout
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{Party, PartyRelationship}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.{Command, GetParty, PartyRelationshipCommand}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val personSeedFormat: RootJsonFormat[PersonSeed]             = jsonFormat3(PersonSeed)
  implicit val personFormat: RootJsonFormat[Person]                     = jsonFormat4(Person)
  implicit val organizationSeedFormat: RootJsonFormat[OrganizationSeed] = jsonFormat6(OrganizationSeed)
  implicit val organizationFormat: RootJsonFormat[Organization]         = jsonFormat7(Organization)
  implicit val relationshipFormat: RootJsonFormat[Relationship]         = jsonFormat5(Relationship)
  implicit val relationshipsFormat: RootJsonFormat[Relationships]       = jsonFormat1(Relationships)
  implicit val problemFormat: RootJsonFormat[Problem]                   = jsonFormat3(Problem)
  implicit val tokenFeedFormat: RootJsonFormat[TokenSeed]               = jsonFormat3(TokenSeed)
  implicit val tokenTextFormat: RootJsonFormat[TokenText]               = jsonFormat1(TokenText)

  implicit class CommandersOps(val commanders: List[EntityRef[Command]]) extends AnyVal {

    def convertToRelationships(
      partyRelationships: List[PartyRelationship]
    )(implicit ec: ExecutionContext, timeout: Timeout): Future[List[Relationship]] =
      Future.traverse(partyRelationships)(commanders.convertToRelationship).map(_.flatten)

    def convertToRelationship(
      partyRelationship: PartyRelationship
    )(implicit ec: ExecutionContext, timeout: Timeout): Future[Option[Relationship]] = {
      val partiesRetrieved: Future[List[(Option[Party], Option[Party])]] = Future.traverse(commanders) { commander =>
        for {
          from <- commander.ask(ref => GetParty(partyRelationship.from, ref))
          to   <- commander.ask(ref => GetParty(partyRelationship.to, ref))
        } yield (from, to)

      }

      partiesRetrieved.map { parties =>
        for {
          from <- parties.find(_._1.isDefined).flatMap(_._1)
          to   <- parties.find(_._2.isDefined).flatMap(_._2)
        } yield Relationship(
          from = from.externalId,
          to = to.externalId,
          role = partyRelationship.role.stringify,
          platformRole = partyRelationship.platformRole,
          status = Some(partyRelationship.status.stringify)
        )
      }
    }

    def getPartyRelationships(
      id: UUID,
      commandFunc: (UUID, ActorRef[List[PartyRelationship]]) => PartyRelationshipCommand
    )(implicit ec: ExecutionContext, timeout: Timeout): Future[List[PartyRelationship]] = {
      Future
        .traverse(commanders)(commander => commander.ask[List[PartyRelationship]](ref => commandFunc(id, ref)))
        .map(_.flatten)
    }

    def getRelationships(
      party: Party,
      commandFunc: (UUID, ActorRef[List[PartyRelationship]]) => PartyRelationshipCommand
    )(implicit ec: ExecutionContext, timeout: Timeout): Future[List[Relationship]] = {
      for {
        partyRelationships <- commanders.getPartyRelationships(party.id, commandFunc)
        relationships      <- commanders.convertToRelationships(partyRelationships)
      } yield relationships
    }
  }

}
