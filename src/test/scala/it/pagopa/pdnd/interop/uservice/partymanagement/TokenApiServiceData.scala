package it.pagopa.pdnd.interop.uservice.partymanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.PersistedPartyRelationshipState.Pending
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{
  Delegate,
  Manager,
  PersistedPartyRelationship,
  PersistedProduct,
  Token
}

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object TokenApiServiceData {

  // format: off

  lazy final val createTokenUuid0 = UUID.fromString("37f8dce0-0a5b-476b-9fdd-a7a658eb9210")
  lazy final val createTokenUuid1 = UUID.fromString("37f8dce0-0a5b-476b-9fdd-a7a658eb9211")

  lazy final val tokenSeedId1 = "47f8dce0-0a5b-476b-9fdd-a7a658eb9210"
  lazy final val tokenSeedId2 = "47f8dce0-0a5b-476b-9fdd-a7a658eb9211"
  lazy final val tokenSeedId3 = "47f8dce0-0a5b-476b-9fdd-a7a658eb9212"

  lazy final val personId1 = UUID.randomUUID()
  lazy final val personId2 = UUID.randomUUID()
  lazy final val personId3 = UUID.randomUUID()

  lazy final val orgId1 = UUID.randomUUID()
  lazy final val orgId2 = UUID.randomUUID()
  lazy final val orgId3 = UUID.randomUUID()

  lazy final val personSeed1 = PersonSeed(id = personId1)
  lazy final val personSeed2 = PersonSeed(id = personId2)
  lazy final val personSeed3 = PersonSeed(id = personId3)

  lazy final val institutionId1 = "id9"
  lazy final val institutionId2 = "id10"
  lazy final val institutionId3 = "id13"

  lazy final val organizationSeed1 = OrganizationSeed(institutionId1, "Institutions Nine", "mail9@mail.org", "taxCode",    attributes = Seq.empty, products = Set.empty)
  lazy final val organizationSeed2 = OrganizationSeed(institutionId2, "Institutions Ten", "mail10@mail.org", "taxCode",    attributes = Seq.empty, products = Set.empty)
  lazy final val organizationSeed3 = OrganizationSeed(institutionId3, "Institutions Eleven", "mail11@mail.org", "taxCode", attributes = Seq.empty, products = Set.empty)

  lazy final val relationshipSeed2 = RelationshipSeed(from = personId1, to = orgId1, role = PartyRole.DELEGATE, product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed1 = RelationshipSeed(from = personId1, to = orgId1, role = PartyRole.MANAGER,  product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed3 = RelationshipSeed(from = personId2, to = orgId2, role = PartyRole.MANAGER,  product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed4 = RelationshipSeed(from = personId2, to = orgId2, role = PartyRole.DELEGATE, product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed5 = RelationshipSeed(from = personId3, to = orgId3, role = PartyRole.MANAGER,  product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed6 = RelationshipSeed(from = personId3, to = orgId3, role = PartyRole.DELEGATE, product = RelationshipProductSeed(id = "p1", role ="admin"))
  
  lazy final val relationshipId1 = UUID.fromString("37f8dce0-0a5b-476b-9fdd-a7a658eb9299")
  lazy final val relationshipId2 = UUID.fromString("37f8dce0-0a5b-476b-9fdd-a7a658eb9298")
  lazy final val relationshipId3 = UUID.fromString("37f8dce0-0a5b-476b-9fdd-a7a658eb9297")
  lazy final val relationshipId4 = UUID.fromString("37f8dce0-0a5b-476b-9fdd-a7a658eb9296")
  lazy final val partyRelationship1 = PersistedPartyRelationship(id = relationshipId1, start = OffsetDateTime.now(), end = None, state = Pending, from = personId2, to = orgId2, role = Manager,  product = PersistedProduct(id = "p1", role = "admin", timestamp = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None)
  lazy final val partyRelationship2 = PersistedPartyRelationship(id = relationshipId2, start = OffsetDateTime.now(), end = None, state = Pending, from = personId2, to = orgId2, role = Delegate, product = PersistedProduct(id = "p1", role = "admin", timestamp = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None)
  lazy final val partyRelationship3 = PersistedPartyRelationship(id = relationshipId3, start = OffsetDateTime.now(), end = None, state = Pending, from = personId3, to = orgId3, role = Manager,  product = PersistedProduct(id = "p1", role = "admin", timestamp = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None)
  lazy final val partyRelationship4 = PersistedPartyRelationship(id = relationshipId4, start = OffsetDateTime.now(), end = None, state = Pending, from = personId3, to = orgId3, role = Delegate, product = PersistedProduct(id = "p1", role = "admin", timestamp = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None)

  lazy val tokenSeed1: TokenSeed = TokenSeed(seed = tokenSeedId2, relationships = RelationshipsSeed(Seq(relationshipSeed3, relationshipSeed4)), "checksum")
  lazy val tokenSeed2: TokenSeed = TokenSeed(seed = tokenSeedId3, relationships = RelationshipsSeed(Seq(relationshipSeed5, relationshipSeed6)), "checksum")

  lazy val token1: Token = Token.generate(tokenSeed1, Seq(partyRelationship1, partyRelationship2)).toOption.get
  lazy val token2: Token = Token.generate(tokenSeed2, Seq(partyRelationship3, partyRelationship4)).toOption.get

  // format: on

  def prepareTest(
    personSeed: PersonSeed,
    organizationSeed: OrganizationSeed,
    relationshipOne: RelationshipSeed,
    relationshipTwo: RelationshipSeed
  )(implicit
    as: ActorSystem,
    mp: Marshaller[PersonSeed, MessageEntity],
    mo: Marshaller[OrganizationSeed, MessageEntity],
    mr: Marshaller[RelationshipSeed, MessageEntity],
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
