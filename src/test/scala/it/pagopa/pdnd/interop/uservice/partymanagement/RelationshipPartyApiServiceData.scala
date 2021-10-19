package it.pagopa.pdnd.interop.uservice.partymanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.{HttpResponse, MessageEntity}
import it.pagopa.pdnd.interop.uservice.partymanagement.model._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object RelationshipPartyApiServiceData {

  def prepareTest(personSeed: PersonSeed, organizationSeed: OrganizationSeed, relationshipSeed: RelationshipSeed)(
    implicit
    as: ActorSystem,
    mp: Marshaller[PersonSeed, MessageEntity],
    mo: Marshaller[OrganizationSeed, MessageEntity],
    mr: Marshaller[RelationshipSeed, MessageEntity],
    ec: ExecutionContext
  ): HttpResponse = {
    val personData = Await.result(Marshal(personSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    val _ = createPerson(personData)

    val organizationData = Await.result(Marshal(organizationSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    val _ = createOrganization(organizationData)

    val rlRequestData = Await.result(Marshal(relationshipSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    createRelationship(rlRequestData)

  }

}
