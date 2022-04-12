package it.pagopa.interop.partymanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model._
import it.pagopa.interop.partymanagement.model._
import it.pagopa.interop.partymanagement.model.party.PersistedPartyRelationshipState.Pending
import it.pagopa.interop.partymanagement.model.party.{
  PersistedPartyRelationship,
  PersistedPartyRole,
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

  lazy final val tokenId1 = UUID.randomUUID()
  lazy final val tokenId2 = UUID.randomUUID()
  lazy final val tokenId3 = UUID.randomUUID()
  lazy final val tokenId4 = UUID.randomUUID()
  lazy final val tokenId5 = UUID.randomUUID()
  lazy final val tokenId6 = UUID.randomUUID()
  lazy final val tokenId7 = UUID.randomUUID()

  lazy final val personId1 = UUID.randomUUID()
  lazy final val personId2 = UUID.randomUUID()
  lazy final val personId3 = UUID.randomUUID()
  lazy final val personId4 = UUID.randomUUID()
  lazy final val personId5 = UUID.randomUUID()
  lazy final val personId6 = UUID.randomUUID()
  lazy final val personId7 = UUID.randomUUID()

  lazy final val orgId1 = UUID.randomUUID()
  lazy final val orgId2 = UUID.randomUUID()
  lazy final val orgId3 = UUID.randomUUID()
  lazy final val orgId4 = UUID.randomUUID()
  lazy final val orgId5 = UUID.randomUUID()
  lazy final val orgId6 = UUID.randomUUID()
  lazy final val orgId7 = UUID.randomUUID()

  lazy final val personSeed1 = PersonSeed(id = personId1)
  lazy final val personSeed2 = PersonSeed(id = personId2)
  lazy final val personSeed3 = PersonSeed(id = personId3)
  lazy final val personSeed4 = PersonSeed(id = personId4)
  lazy final val personSeed5 = PersonSeed(id = personId5)
  lazy final val personSeed6 = PersonSeed(id = personId6)
  lazy final val personSeed7 = PersonSeed(id = personId7)

  lazy final val institutionId1 = "id9"
  lazy final val institutionId2 = "id10"
  lazy final val institutionId3 = "id13"
  lazy final val institutionId4 = "id14"
  lazy final val institutionId5 = "id15"
  lazy final val institutionId6 = "id16"
  lazy final val institutionId7 = "id17"

  lazy final val institutionSeed1 = InstitutionSeed(
    institutionId = institutionId1,
    description = "Institutions Nine",
    digitalAddress = "mail9@mail.org",
    address = "address1",
    zipCode = "zipCode1",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Set.empty,
    origin = "IPA",
    institutionType = "PA"
  )
  lazy final val institutionSeed2 = InstitutionSeed(
    institutionId = institutionId2,
    description = "Institutions Ten",
    digitalAddress = "mail10@mail.org",
    address = "address2",
    zipCode = "zipCode2",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Set.empty,
    origin = "IPA",
    institutionType = "PA"
  )
  lazy final val institutionSeed3 = InstitutionSeed(
    institutionId = institutionId3,
    description = "Institutions Eleven",
    digitalAddress = "mail11@mail.org",
    address = "address3",
    zipCode = "zipCode3",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Set.empty,
    origin = "IPA",
    institutionType = "PA"
  )
  lazy final val institutionSeed4 = InstitutionSeed(
    institutionId = institutionId4,
    description = "Institutions Twelve",
    digitalAddress = "mail11@mail.org",
    address = "address4",
    zipCode = "zipCode4",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Set.empty,
    origin = "IPA",
    institutionType = "PA"
  )
  lazy final val institutionSeed5 = InstitutionSeed(
    institutionId = institutionId5,
    description = "Institutions Fifteen",
    digitalAddress = "mail15@mail.org",
    address = "address4",
    zipCode = "zipCode4",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Set.empty,
    origin = "IPA",
    institutionType = "PA"
  )
  lazy final val institutionSeed6 = InstitutionSeed(
    institutionId = institutionId6,
    description = "Institutions Sixteen",
    digitalAddress = "mail15@mail.org",
    address = "address5",
    zipCode = "zipCode5",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Set.empty,
    origin = "IPA",
    institutionType = "PA"
  )
  lazy final val institutionSeed7 = InstitutionSeed(
    institutionId = institutionId7,
    description = "Institutions Seventeen",
    digitalAddress = "mail15@mail.org",
    address = "address6",
    zipCode = "zipCode6",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Set.empty,
    origin = "IPA",
    institutionType = "PA"
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
  lazy final val relationshipSeed9 = RelationshipSeed(from = personId5, to = orgId5, role = PartyRole.MANAGER,  product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed10 = RelationshipSeed(from = personId5, to = orgId5, role = PartyRole.DELEGATE, product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed11 = RelationshipSeed(from = personId6, to = orgId6, role = PartyRole.MANAGER,  product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed12 = RelationshipSeed(from = personId6, to = orgId6, role = PartyRole.DELEGATE, product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed13 = RelationshipSeed(from = personId7, to = orgId7, role = PartyRole.MANAGER,  product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed14 = RelationshipSeed(from = personId7, to = orgId7, role = PartyRole.DELEGATE, product = RelationshipProductSeed(id = "p1", role ="admin"))

  lazy final val relationship1 = Relationship(id = UUID.randomUUID(), from = personId2, to = orgId2, role = PartyRole.MANAGER,  product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship2 = Relationship(id = UUID.randomUUID(), from = personId2, to = orgId2, role = PartyRole.DELEGATE, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship3 = Relationship(id = UUID.randomUUID(), from = personId3, to = orgId3, role = PartyRole.MANAGER,  product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship4 = Relationship(id = UUID.randomUUID(), from = personId3, to = orgId3, role = PartyRole.DELEGATE, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship5 = Relationship(id = UUID.randomUUID(), from = personId4, to = orgId4, role = PartyRole.MANAGER, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship6 = Relationship(id = UUID.randomUUID(), from = personId4, to = orgId4, role = PartyRole.DELEGATE, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship7 = Relationship(id = UUID.randomUUID(), from = personId5, to = orgId5, role = PartyRole.MANAGER, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship8 = Relationship(id = UUID.randomUUID(), from = personId5, to = orgId5, role = PartyRole.DELEGATE, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship9 = Relationship(id = UUID.randomUUID(), from = personId6, to = orgId6, role = PartyRole.MANAGER, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship10 = Relationship(id = UUID.randomUUID(), from = personId6, to = orgId6, role = PartyRole.DELEGATE, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship11 = Relationship(id = UUID.randomUUID(), from = personId7, to = orgId7, role = PartyRole.MANAGER, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship12 = Relationship(id = UUID.randomUUID(), from = personId7, to = orgId7, role = PartyRole.DELEGATE, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)

  lazy final val relationshipId1 = UUID.randomUUID()
  lazy final val relationshipId2 = UUID.randomUUID()
  lazy final val relationshipId3 = UUID.randomUUID()
  lazy final val relationshipId4 = UUID.randomUUID()
  lazy final val relationshipId5 =UUID.randomUUID()
  lazy final val relationshipId6 = UUID.randomUUID()
  lazy final val relationshipId7 = UUID.randomUUID()
  lazy final val relationshipId8 = UUID.randomUUID()
  lazy final val relationshipId9 = UUID.randomUUID()
  lazy final val relationshipId10 = UUID.randomUUID()
  lazy final val relationshipId11 = UUID.randomUUID()
  lazy final val relationshipId12 = UUID.randomUUID()

  lazy final val partyRelationship1 = PersistedPartyRelationship(id = relationshipId1, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId2, to = orgId2, role = PersistedPartyRole.Manager,  product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None, pricingPlan = None, institutionUpdate = None, billing = None)
  lazy final val partyRelationship2 = PersistedPartyRelationship(id = relationshipId2, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId2, to = orgId2, role = PersistedPartyRole.Delegate, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None, pricingPlan = None, institutionUpdate = None, billing = None)
  lazy final val partyRelationship3 = PersistedPartyRelationship(id = relationshipId3, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId3, to = orgId3, role = PersistedPartyRole.Manager,  product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None, pricingPlan = None, institutionUpdate = None, billing = None)
  lazy final val partyRelationship4 = PersistedPartyRelationship(id = relationshipId4, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId3, to = orgId3, role = PersistedPartyRole.Delegate, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None, pricingPlan = None, institutionUpdate = None, billing = None)
  lazy final val partyRelationship5 = PersistedPartyRelationship(id = relationshipId5, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId4, to = orgId4, role = PersistedPartyRole.Manager, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None, pricingPlan = None, institutionUpdate = None, billing = None)
  lazy final val partyRelationship6 = PersistedPartyRelationship(id = relationshipId6, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId4, to = orgId4, role = PersistedPartyRole.Delegate, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None, pricingPlan = None, institutionUpdate = None, billing = None)
  lazy final val partyRelationship7 = PersistedPartyRelationship(id = relationshipId7, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId5, to = orgId5, role = PersistedPartyRole.Manager, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None, pricingPlan = None, institutionUpdate = None, billing = None)
  lazy final val partyRelationship8 = PersistedPartyRelationship(id = relationshipId8, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId5, to = orgId5, role = PersistedPartyRole.Delegate, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None, pricingPlan = None, institutionUpdate = None, billing = None)
  lazy final val partyRelationship9 = PersistedPartyRelationship(id = relationshipId9, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId6, to = orgId6, role = PersistedPartyRole.Manager, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None, pricingPlan = None, institutionUpdate = None, billing = None)
  lazy final val partyRelationship10 = PersistedPartyRelationship(id = relationshipId10, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId6, to = orgId6, role = PersistedPartyRole.Delegate, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None, pricingPlan = None, institutionUpdate = None, billing = None)
  lazy final val partyRelationship11 = PersistedPartyRelationship(id = relationshipId11, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId7, to = orgId7, role = PersistedPartyRole.Manager, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None, pricingPlan = None, institutionUpdate = None, billing = None)
  lazy final val partyRelationship12 = PersistedPartyRelationship(id = relationshipId12, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId7, to = orgId7, role = PersistedPartyRole.Delegate, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None, pricingPlan = None, institutionUpdate = None, billing = None)

  lazy val tokenSeed1: TokenSeed = TokenSeed(id = tokenId2.toString, relationships = Relationships(Seq(relationship1, relationship2)), "checksum", OnboardingContractInfo("test", "test"))
  lazy val tokenSeed2: TokenSeed = TokenSeed(id = tokenId3.toString, relationships = Relationships(Seq(relationship3, relationship4)), "checksum", OnboardingContractInfo("test", "test"))
  lazy val tokenSeed3: TokenSeed = TokenSeed(id = tokenId4.toString, relationships = Relationships(Seq(relationship5, relationship6)), "checksum", OnboardingContractInfo("test", "test"))
  lazy val tokenSeed4: TokenSeed = TokenSeed(id = tokenId5.toString, relationships = Relationships(Seq(relationship7, relationship8)), "checksum", OnboardingContractInfo("test", "test"))
  lazy val tokenSeed5: TokenSeed = TokenSeed(id = tokenId6.toString, relationships = Relationships(Seq(relationship9, relationship10)), "checksum", OnboardingContractInfo("test", "test"))
  lazy val tokenSeed6: TokenSeed = TokenSeed(id = tokenId7.toString, relationships = Relationships(Seq(relationship11, relationship12)), "checksum", OnboardingContractInfo("test", "test"))

  lazy val token1: Token = Token.generate(tokenSeed1, Seq(partyRelationship1, partyRelationship2),timestampValid).toOption.get
  lazy val token2: Token = Token.generate(tokenSeed2, Seq(partyRelationship3, partyRelationship4),timestampValid).toOption.get
  lazy val token3: Token = Token.generate(tokenSeed3, Seq(partyRelationship5, partyRelationship6),timestampValid).toOption.get
  lazy val token4: Token = Token.generate(tokenSeed4, Seq(partyRelationship7, partyRelationship8),timestampValid).toOption.get
  lazy val token5: Token = Token.generate(tokenSeed5, Seq(partyRelationship9, partyRelationship10),timestampValid).toOption.get
  lazy val token6: Token = Token.generate(tokenSeed6, Seq(partyRelationship11, partyRelationship12),timestampValid).toOption.get

  // format: on

  def prepareTest(
    personSeed: PersonSeed,
    institutionSeed: InstitutionSeed,
    relationshipOne: RelationshipSeed,
    relationshipTwo: RelationshipSeed
  )(implicit
    as: ActorSystem,
    mp: Marshaller[PersonSeed, MessageEntity],
    mo: Marshaller[InstitutionSeed, MessageEntity],
    mr: Marshaller[RelationshipSeed, MessageEntity],
    ec: ExecutionContext
  ): HttpResponse = {
    val personRequestData = Await.result(Marshal(personSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    val _ = createPerson(personRequestData)

    val orgRequestData = Await.result(Marshal(institutionSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    val _ = createInstitution(orgRequestData)

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
