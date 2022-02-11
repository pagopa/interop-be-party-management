package it.pagopa.pdnd.interop.uservice.partymanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.PersistedPartyRelationshipState.Pending
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{
  PersistedPartyRole,
  PersistedPartyRelationship,
  PersistedProduct,
  Token
}

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object TokenApiServiceData {

  lazy final val createTokenUuid0 = UUID.fromString("37f8dce0-0a5b-476b-9fdd-a7a658eb9210")
  lazy final val createTokenUuid1 = UUID.fromString("37f8dce0-0a5b-476b-9fdd-a7a658eb9211")

  lazy final val tokenId1 = "47f8dce0-0a5b-476b-9fdd-a7a658eb9210"
  lazy final val tokenId2 = "47f8dce0-0a5b-476b-9fdd-a7a658eb9211"
  lazy final val tokenId3 = "47f8dce0-0a5b-476b-9fdd-a7a658eb9212"
  lazy final val tokenId4 = "47f8dce0-0a5b-476b-9fdd-a7a658eb9212"

  lazy final val personId1 = UUID.randomUUID()
  lazy final val personId2 = UUID.randomUUID()
  lazy final val personId3 = UUID.randomUUID()
  lazy final val personId4 = UUID.randomUUID()

  lazy final val orgId1 = UUID.randomUUID()
  lazy final val orgId2 = UUID.randomUUID()
  lazy final val orgId3 = UUID.randomUUID()
  lazy final val orgId4 = UUID.randomUUID()

  lazy final val personSeed1 = PersonSeed(id = personId1)
  lazy final val personSeed2 = PersonSeed(id = personId2)
  lazy final val personSeed3 = PersonSeed(id = personId3)
  lazy final val personSeed4 = PersonSeed(id = personId4)

  lazy final val institutionId1 = "id9"
  lazy final val institutionId2 = "id10"
  lazy final val institutionId3 = "id13"
  lazy final val institutionId4 = "id14"

  lazy final val organizationSeed1 = OrganizationSeed(
    institutionId = institutionId1,
    description = "Institutions Nine",
    digitalAddress = "mail9@mail.org",
    address = "address1",
    zipCode = "zipCode1",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Set.empty
  )
  lazy final val organizationSeed2 = OrganizationSeed(
    institutionId = institutionId2,
    description = "Institutions Ten",
    digitalAddress = "mail10@mail.org",
    address = "address2",
    zipCode = "zipCode2",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Set.empty
  )
  lazy final val organizationSeed3 = OrganizationSeed(
    institutionId = institutionId3,
    description = "Institutions Eleven",
    digitalAddress = "mail11@mail.org",
    address = "address3",
    zipCode = "zipCode3",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Set.empty
  )
  lazy final val organizationSeed4 = OrganizationSeed(
    institutionId = institutionId4,
    description = "Institutions Twelve",
    digitalAddress = "mail11@mail.org",
    address = "address4",
    zipCode = "zipCode4",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Set.empty
  )
  // format: off
  lazy final val relationshipSeed2 = RelationshipSeed(from = personId1, to = orgId1, role = PartyRole.DELEGATE, product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed1 = RelationshipSeed(from = personId1, to = orgId1, role = PartyRole.MANAGER,  product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed3 = RelationshipSeed(from = personId2, to = orgId2, role = PartyRole.MANAGER,  product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed4 = RelationshipSeed(from = personId2, to = orgId2, role = PartyRole.DELEGATE, product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed5 = RelationshipSeed(from = personId3, to = orgId3, role = PartyRole.MANAGER,  product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed6 = RelationshipSeed(from = personId3, to = orgId3, role = PartyRole.DELEGATE, product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed7 = RelationshipSeed(from = personId4, to = orgId4, role = PartyRole.MANAGER,  product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed8 = RelationshipSeed(from = personId4, to = orgId4, role = PartyRole.DELEGATE, product = RelationshipProductSeed(id = "p1", role ="admin"))

  lazy final val relationship1 = Relationship(id = UUID.randomUUID(), from = personId2, to = orgId2, role = PartyRole.MANAGER,  product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship2 = Relationship(id = UUID.randomUUID(), from = personId2, to = orgId2, role = PartyRole.DELEGATE, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship3 = Relationship(id = UUID.randomUUID(), from = personId3, to = orgId3, role = PartyRole.MANAGER,  product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship4 = Relationship(id = UUID.randomUUID(), from = personId3, to = orgId3, role = PartyRole.DELEGATE, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship5 = Relationship(id = UUID.randomUUID(), from = personId4, to = orgId4, role = PartyRole.MANAGER, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship6 = Relationship(id = UUID.randomUUID(), from = personId4, to = orgId4, role = PartyRole.DELEGATE, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)

  lazy final val relationshipId1 = UUID.fromString("37f8dce0-0a5b-476b-9fdd-a7a658eb9299")
  lazy final val relationshipId2 = UUID.fromString("37f8dce0-0a5b-476b-9fdd-a7a658eb9298")
  lazy final val relationshipId3 = UUID.fromString("37f8dce0-0a5b-476b-9fdd-a7a658eb9297")
  lazy final val relationshipId4 = UUID.fromString("37f8dce0-0a5b-476b-9fdd-a7a658eb9296")
  lazy final val relationshipId5 = UUID.fromString("37f8dce0-0a5b-476b-9fdd-a7a658eb9276")
  lazy final val relationshipId6 = UUID.fromString("37f8dce0-0a5b-476b-9fdd-a7a658eb9277")

  lazy final val partyRelationship1 = PersistedPartyRelationship(id = relationshipId1, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId2, to = orgId2, role = PersistedPartyRole.Manager,  product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None)
  lazy final val partyRelationship2 = PersistedPartyRelationship(id = relationshipId2, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId2, to = orgId2, role = PersistedPartyRole.Delegate, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None)
  lazy final val partyRelationship3 = PersistedPartyRelationship(id = relationshipId3, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId3, to = orgId3, role = PersistedPartyRole.Manager,  product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None)
  lazy final val partyRelationship4 = PersistedPartyRelationship(id = relationshipId4, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId3, to = orgId3, role = PersistedPartyRole.Delegate, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None)
  lazy final val partyRelationship5 = PersistedPartyRelationship(id = relationshipId5, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId4, to = orgId4, role = PersistedPartyRole.Manager, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None)
  lazy final val partyRelationship6 = PersistedPartyRelationship(id = relationshipId6, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId4, to = orgId4, role = PersistedPartyRole.Delegate, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None)

  lazy val tokenSeed1: TokenSeed = TokenSeed(id = tokenId2, relationships = Relationships(Seq(relationship1, relationship2)), "checksum", OnboardingContractInfo("test", "test"))
  lazy val tokenSeed2: TokenSeed = TokenSeed(id = tokenId3, relationships = Relationships(Seq(relationship3, relationship4)), "checksum", OnboardingContractInfo("test", "test"))
  lazy val tokenSeed3: TokenSeed = TokenSeed(id = tokenId4, relationships = Relationships(Seq(relationship5, relationship6)), "checksum", OnboardingContractInfo("test", "test"))

  lazy val token1: Token = Token.generate(tokenSeed1, Seq(partyRelationship1, partyRelationship2),timestampValid).toOption.get
  lazy val token2: Token = Token.generate(tokenSeed2, Seq(partyRelationship3, partyRelationship4),timestampValid).toOption.get
  lazy val token3: Token = Token.generate(tokenSeed3, Seq(partyRelationship5, partyRelationship6),timestampValid).toOption.get

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
