package it.pagopa.pdnd.interop.uservice.partymanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.{HttpResponse, MessageEntity}
import it.pagopa.pdnd.interop.uservice.partymanagement.model._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object RelationshipPartyApiServiceData {

//  lazy final val uuid1 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9219")
//  lazy final val uuid2 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9220")
//  lazy final val uuid3 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9221")
//  lazy final val uuid5 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9223")
//  lazy final val uuid6 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9224")
//  lazy final val uuid7 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9225")
//  lazy final val uuid8 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9226")
//  lazy final val uuid9 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9227")

//  lazy final val institutionId1 = "id5"
//  lazy final val institutionId2 = "id6"
//  lazy final val institutionId3 = "id7"
//  lazy final val institutionId4 = "id8"
//  lazy final val institutionId5 = "id9"
//  lazy final val institutionId6 = "id20"

//  lazy final val personSeed1 = PersonSeed(taxCode = taxCode1, surname = "Ripley", name = "Ellen")
//  lazy final val personSeed2 = PersonSeed(taxCode = taxCode2, surname = "Onizuka", name = "Eikichi")
//  lazy final val personSeed3 = PersonSeed(taxCode = taxCode3, surname = "Murphy", name = "Alex")
//  lazy final val personSeed4 = PersonSeed(taxCode = taxCode4, surname = "Cartman", name = "Eric")
//  lazy final val personSeed5 = PersonSeed(taxCode = taxCode5, surname = "Wick", name = "John")
//  lazy final val personSeed6 = PersonSeed(taxCode = taxCode6, surname = "Durden", name = "Tyler")
//  lazy final val personSeed7 = PersonSeed(taxCode = taxCode7, surname = "...", name = "...")
//  lazy final val personSeed8 = PersonSeed(taxCode = taxCode8, surname = "....", name = "....")

//  lazy final val orgSeed1 = OrganizationSeed(institutionId1, "Institutions Five", "mail5@mail.org", Seq.empty)
//  lazy final val orgSeed2 = OrganizationSeed(institutionId2, "Institutions Six", "mail6@mail.org", Seq.empty)
//  lazy final val orgSeed3 = OrganizationSeed(institutionId3, "Institutions Seven", "mail7@mail.org", Seq.empty)
//  lazy final val orgSeed4 = OrganizationSeed(institutionId4, "Institutions Eight", "mail8@mail.org", Seq.empty)
//  lazy final val orgSeed5 = OrganizationSeed(institutionId5, "Institutions Nine", "mail9@mail.org", Seq.empty)
//  lazy final val orgSeed6 = OrganizationSeed(institutionId6, "Institutions Ten", "mail10@mail.org", Seq.empty)

//  lazy final val rlSeed1 = RelationshipSeed(from = taxCode1, to = institutionId1, role = "Manager", "admin")
//  lazy final val rlSeed2 = RelationshipSeed(from = taxCode2, to = institutionId2, role = "Manager", "admin")
//  lazy final val rlSeed3 = RelationshipSeed(from = taxCode3, to = institutionId3, role = "Manager", "admin")
//  lazy final val rlSeed4 = RelationshipSeed(from = taxCode4, to = institutionId4, role = "Manager", "admin")
//  lazy final val rlSeed5 = RelationshipSeed(from = taxCode5, to = institutionId4, role = "Delegate", "admin")
//  lazy final val rlSeed6 = RelationshipSeed(from = taxCode6, to = institutionId5, role = "Manager", "admin")
//  lazy final val rlSeed7 = RelationshipSeed(from = taxCode7, to = institutionId6, role = "Manager", "admin")
//  lazy final val rlSeed8 = RelationshipSeed(from = taxCode8, to = institutionId6, role = "Delegate", "security")

//  lazy final val rlExpected2 = Relationships(
//    Seq(
//      Relationship(
//        id = UUID.randomUUID(),
//        from = taxCode2,
//        to = institutionId2,
//        role = "Manager",
//        platformRole = "admin",
//        status = "Pending",
//        filePath = None,
//        fileName = None,
//        contentType = None
//      )
//    )
//  )
//  lazy final val rlExpected3 = Relationships(
//    Seq(
//      Relationship(
//        id = UUID.randomUUID(),
//        from = taxCode4,
//        to = institutionId4,
//        role = "Manager",
//        platformRole = "admin",
//        status = "Pending",
//        filePath = None,
//        fileName = None,
//        contentType = None
//      ),
//      Relationship(
//        id = UUID.randomUUID(),
//        from = taxCode5,
//        to = institutionId4,
//        role = "Delegate",
//        platformRole = "admin",
//        status = "Pending",
//        filePath = None,
//        fileName = None,
//        contentType = None
//      )
//    )
//  )

//  lazy final val rlExpected4 = Relationships(
//    Seq(
//      Relationship(
//        id = UUID.randomUUID(),
//        from = taxCode8,
//        to = institutionId6,
//        role = "Delegate",
//        platformRole = "security",
//        status = "Pending",
//        filePath = None,
//        fileName = None,
//        contentType = None
//      )
//    )
//  )

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
