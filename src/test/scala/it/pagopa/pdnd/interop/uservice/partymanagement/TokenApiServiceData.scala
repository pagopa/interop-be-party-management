package it.pagopa.pdnd.interop.uservice.partymanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{Delegate, Manager, PartyRelationshipId, Token}

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object TokenApiServiceData {

  // format: off

  lazy final val createTokenUuid0 = "37f8dce0-0a5b-476b-9fdd-a7a658eb9210"
  lazy final val createTokenUuid1 = "37f8dce0-0a5b-476b-9fdd-a7a658eb9211"
  lazy final val createTokenUuid2 = "37f8dce0-0a5b-476b-9fdd-a7a658eb9212"
  lazy final val createTokenUuid3 = "37f8dce0-0a5b-476b-9fdd-a7a658eb9213"
  lazy final val createTokenUuid4 = "37f8dce0-0a5b-476b-9fdd-a7a658eb9214"
  lazy final val createTokenUuid5 = "37f8dce0-0a5b-476b-9fdd-a7a658eb9215"

  lazy final val tokenSeedId1 = "47f8dce0-0a5b-476b-9fdd-a7a658eb9210"
  lazy final val tokenSeedId2 = "47f8dce0-0a5b-476b-9fdd-a7a658eb9211"
  lazy final val tokenSeedId3 = "47f8dce0-0a5b-476b-9fdd-a7a658eb9212"

  lazy final val taxCode1 = "RSSMRA75L01H501H"
  lazy final val taxCode2 = "RSSMRA75L01H501I"
  lazy final val taxCode3 = "RSSMRA75L01H501J"

  lazy final val personSeed1 = PersonSeed(taxCode = taxCode1, surname = "Mascetti", name = "Raffaello")
  lazy final val personSeed2 = PersonSeed(taxCode = taxCode2, surname = "Melandri", name = "Ranbaudo")
  lazy final val personSeed3 = PersonSeed(taxCode = taxCode3, surname = "Perozzi", name = "Giorgio")

  lazy final val institutionId1 = "id9"
  lazy final val institutionId2 = "id10"
  lazy final val institutionId3 = "id13"

  lazy final val organizationSeed1 = OrganizationSeed(institutionId1, "Institutions Nine", "Raffaello","Mascetti", "mail9@mail.org", Seq.empty)
  lazy final val organizationSeed2 = OrganizationSeed(institutionId2, "Institutions Ten", "Melandri","Ranbaudo", "mail10@mail.org", Seq.empty)
  lazy final val organizationSeed3 = OrganizationSeed(institutionId3, "Institutions Eleven", "Perozzi","Giorgio", "mail11@mail.org", Seq.empty)

  lazy final val relationship1 = Relationship(from = taxCode1, to = institutionId1, role = "Manager",  "admin", None)
  lazy final val relationship2 = Relationship(from = taxCode1, to = institutionId1, role = "Delegate", "admin", None)
  lazy final val relationship3 = Relationship(from = taxCode2, to = institutionId2, role = "Manager",  "admin", None)
  lazy final val relationship4 = Relationship(from = taxCode2, to = institutionId2, role = "Delegate", "admin", None)
  lazy final val relationship5 = Relationship(from = taxCode3, to = institutionId3, role = "Manager",  "admin", None)
  lazy final val relationship6 = Relationship(from = taxCode3, to = institutionId3, role = "Delegate", "admin", None)

  lazy final val partyRelationshipId1 = PartyRelationshipId(from = UUID.fromString(createTokenUuid2), to = UUID.fromString(createTokenUuid3), role = Manager, "admin")
  lazy final val partyRelationshipId2 = PartyRelationshipId(from = UUID.fromString(createTokenUuid2), to = UUID.fromString(createTokenUuid3), role = Delegate, "admin")
  lazy final val partyRelationshipId3 = PartyRelationshipId(from = UUID.fromString(createTokenUuid4), to = UUID.fromString(createTokenUuid5), role = Manager, "admin")
  lazy final val partyRelationshipId4 = PartyRelationshipId(from = UUID.fromString(createTokenUuid4), to = UUID.fromString(createTokenUuid5), role = Delegate, "admin")

  lazy val tokenSeed1: TokenSeed = TokenSeed(seed = tokenSeedId2, relationships = Relationships(Seq(relationship3, relationship4)), "checksum")
  lazy val tokenSeed2: TokenSeed = TokenSeed(seed = tokenSeedId3, relationships = Relationships(Seq(relationship5, relationship6)), "checksum")

  lazy val token1: Token = Token.generate(tokenSeed1, Seq(partyRelationshipId1, partyRelationshipId2)).toOption.get
  lazy val token2: Token = Token.generate(tokenSeed2, Seq(partyRelationshipId3, partyRelationshipId4)).toOption.get

  def prepareTest(
                   personSeed: PersonSeed,
                   organizationSeed: OrganizationSeed,
                   relationshipOne: Relationship,
                   relationshipTwo: Relationship
                 )(implicit
                   as: ActorSystem,
                   mp: Marshaller[PersonSeed, MessageEntity],
                   mo: Marshaller[OrganizationSeed, MessageEntity],
                   mr: Marshaller[Relationship, MessageEntity],
                   ec: ExecutionContext
                 ): HttpResponse = {
    val personRequestData = Await.result(Marshal(personSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    val _ = createPerson(personRequestData)

    val orgRequestData = Await.result(Marshal(organizationSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    val _ = createOrganization(orgRequestData)

    val rlRequestData1 = Await.result(Marshal(relationshipOne).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    val _ = createRelationship(rlRequestData1)

    val rlRequestData2 = Await.result(Marshal(relationshipTwo).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    val _ = createRelationship(rlRequestData2)

    Await.result(
      Http().singleRequest(
        HttpRequest(
          uri = s"$url/relationships?from=${relationshipOne.from}",
          method = HttpMethods.GET,
          headers = authorization
        )
      ),
      Duration.Inf
    )

  }

}