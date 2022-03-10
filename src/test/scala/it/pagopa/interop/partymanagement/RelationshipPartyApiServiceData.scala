package it.pagopa.interop.partymanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.{HttpResponse, MessageEntity}
import it.pagopa.interop.partymanagement.model._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object RelationshipPartyApiServiceData {

  def prepareTest(personSeed: PersonSeed, institutionSeed: InstitutionSeed, relationshipSeed: RelationshipSeed)(implicit
    as: ActorSystem,
    mp: Marshaller[PersonSeed, MessageEntity],
    mo: Marshaller[InstitutionSeed, MessageEntity],
    mr: Marshaller[RelationshipSeed, MessageEntity],
    ec: ExecutionContext
  ): HttpResponse = {
    val personData = Await.result(Marshal(personSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    val _ = createPerson(personData)

    val institutionData = Await.result(Marshal(institutionSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    val _ = createInstitution(institutionData)

    val rlRequestData = Await.result(Marshal(relationshipSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    createRelationship(rlRequestData)

  }

}
