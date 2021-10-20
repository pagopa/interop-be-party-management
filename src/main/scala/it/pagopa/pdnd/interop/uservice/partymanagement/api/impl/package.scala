package it.pagopa.pdnd.interop.uservice.partymanagement.api

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.util.Timeout
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{Party, PartyRelationship}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.{Command, GetParty, PartyRelationshipCommand}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
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
  implicit val organizationSeedFormat: RootJsonFormat[OrganizationSeed]   = jsonFormat4(OrganizationSeed)
  implicit val organizationFormat: RootJsonFormat[Organization]           = jsonFormat5(Organization)
  implicit val relationshipFormat: RootJsonFormat[Relationship]           = jsonFormat9(Relationship)
  implicit val relationshipSeedFormat: RootJsonFormat[RelationshipSeed]   = jsonFormat4(RelationshipSeed)
  implicit val relationshipsFormat: RootJsonFormat[Relationships]         = jsonFormat1(Relationships)
  implicit val relationshipsSeedFormat: RootJsonFormat[RelationshipsSeed] = jsonFormat1(RelationshipsSeed)
  implicit val problemFormat: RootJsonFormat[Problem]                     = jsonFormat3(Problem)
  implicit val tokenFeedFormat: RootJsonFormat[TokenSeed]                 = jsonFormat3(TokenSeed)
  implicit val tokenTextFormat: RootJsonFormat[TokenText]                 = jsonFormat1(TokenText)
  implicit val bulkPartiesSeedFormat: RootJsonFormat[BulkPartiesSeed]     = jsonFormat1(BulkPartiesSeed)
  implicit val bulkOrganizationsFormat: RootJsonFormat[BulkOrganizations] = jsonFormat2(BulkOrganizations)

  implicit class CommandersOps(val commanders: List[EntityRef[Command]]) extends AnyVal {

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
          id = partyRelationship.id,
          from = from.id,
          to = to.id,
          role = partyRelationship.role.stringify,
          platformRole = partyRelationship.platformRole,
          status = partyRelationship.status.stringify,
          filePath = partyRelationship.filePath,
          fileName = partyRelationship.fileName,
          contentType = partyRelationship.contentType
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

  }

}
