package it.pagopa.pdnd.interop.uservice.partymanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.{HttpResponse, MessageEntity}
import it.pagopa.pdnd.interop.uservice.partymanagement.model._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object RelationshipPartyApiServiceData {

  lazy final val rlUuid1 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9219"
  lazy final val rlUuid2 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9220"
  lazy final val rlUuid3 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9221"
  lazy final val rlUuid4 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9222"
  lazy final val rlUuid5 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9223"
  lazy final val rlUuid6 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9224"

  lazy final val taxCode1 = "RSSMRA75L01H501E"
  lazy final val taxCode2 = "RSSMRA75L01H501F"
  lazy final val taxCode3 = "RSSMRA75L01H501G"

  lazy final val institutionId1 = "id5"
  lazy final val institutionId2 = "id6"
  lazy final val institutionId3 = "id7"

  lazy final val personSeed1 = PersonSeed(taxCode = taxCode1, surname = "Ripley", name = "Ellen")
  lazy final val personSeed2 = PersonSeed(taxCode = taxCode2, surname = "Onizuka", name = "Eikichi")
  lazy final val personSeed3 = PersonSeed(taxCode = taxCode3, surname = "Murphy", name = "Alex")

  lazy final val orgSeed1 =
    OrganizationSeed(institutionId1, "Institutions Five", "Ellen Ripley", "mail5@mail.org", Seq.empty)
  lazy final val orgSeed2 =
    OrganizationSeed(institutionId2, "Institutions Six", "Eikichi Onizuka", "mail6@mail.org", Seq.empty)
  lazy final val orgSeed3 =
    OrganizationSeed(institutionId3, "Institutions Seven", "Alex Murphy", "mail7@mail.org", Seq.empty)

  lazy final val rlSeed1 = RelationShip(from = taxCode1, to = institutionId1, role = "Manager", None)
  lazy final val rlSeed2 = RelationShip(from = taxCode2, to = institutionId2, role = "Manager", None)
  lazy final val rlSeed3 = RelationShip(from = taxCode3, to = institutionId3, role = "Manager", None)

  lazy final val rlExpected1 = RelationShips(Seq.empty)
  lazy final val rlExpected2 = RelationShips(
    Seq(RelationShip(from = taxCode2, to = institutionId2, role = "Manager", status = Some("Pending")))
  )

  def prepareTest(personSeed: PersonSeed, organizationSeed: OrganizationSeed, relationShip: RelationShip)(implicit
    as: ActorSystem,
    mp: Marshaller[PersonSeed, MessageEntity],
    mo: Marshaller[OrganizationSeed, MessageEntity],
    mr: Marshaller[RelationShip, MessageEntity],
    ec: ExecutionContext
  ): HttpResponse = {
    val personData = Await.result(Marshal(personSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    val _ = createPerson(personData)

    val organizationData = Await.result(Marshal(organizationSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    val _ = createOrganization(organizationData)

    val rlRequestData = Await.result(Marshal(relationShip).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    createRelationShip(rlRequestData)

  }

}
