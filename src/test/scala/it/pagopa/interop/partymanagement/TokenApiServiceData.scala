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

  lazy final val tokenId1  = UUID.randomUUID()
  lazy final val tokenId2  = UUID.randomUUID()
  lazy final val tokenId3  = UUID.randomUUID()
  lazy final val tokenId4  = UUID.randomUUID()
  lazy final val tokenId5  = UUID.randomUUID()
  lazy final val tokenId6  = UUID.randomUUID()
  lazy final val tokenId7  = UUID.randomUUID()
  lazy final val tokenId8  = UUID.randomUUID()
  lazy final val tokenId9  = UUID.randomUUID()
  lazy final val tokenId10 = UUID.randomUUID()

  lazy final val personId1  = UUID.randomUUID()
  lazy final val personId2  = UUID.randomUUID()
  lazy final val personId3  = UUID.randomUUID()
  lazy final val personId4  = UUID.randomUUID()
  lazy final val personId5  = UUID.randomUUID()
  lazy final val personId6  = UUID.randomUUID()
  lazy final val personId7  = UUID.randomUUID()
  lazy final val personId8  = UUID.randomUUID()
  lazy final val personId9  = UUID.randomUUID()
  lazy final val personId10 = UUID.randomUUID()

  lazy final val orgId1  = UUID.randomUUID()
  lazy final val orgId2  = UUID.randomUUID()
  lazy final val orgId3  = UUID.randomUUID()
  lazy final val orgId4  = UUID.randomUUID()
  lazy final val orgId5  = UUID.randomUUID()
  lazy final val orgId6  = UUID.randomUUID()
  lazy final val orgId7  = UUID.randomUUID()
  lazy final val orgId8  = UUID.randomUUID()
  lazy final val orgId9  = UUID.randomUUID()
  lazy final val orgId10 = UUID.randomUUID()

  lazy final val personSeed1  = PersonSeed(id = personId1)
  lazy final val personSeed2  = PersonSeed(id = personId2)
  lazy final val personSeed3  = PersonSeed(id = personId3)
  lazy final val personSeed4  = PersonSeed(id = personId4)
  lazy final val personSeed5  = PersonSeed(id = personId5)
  lazy final val personSeed6  = PersonSeed(id = personId6)
  lazy final val personSeed7  = PersonSeed(id = personId7)
  lazy final val personSeed8  = PersonSeed(id = personId8)
  lazy final val personSeed10 = PersonSeed(id = personId10)

  lazy final val externalId1  = "ext_id9"
  lazy final val originId1    = "origin_id9"
  lazy final val externalId2  = "ext_id10"
  lazy final val originId2    = "origin_id10"
  lazy final val externalId3  = "ext_id13"
  lazy final val originId3    = "origin_id13"
  lazy final val externalId4  = "ext_id14"
  lazy final val originId4    = "origin_id14"
  lazy final val externalId5  = "ext_id15"
  lazy final val originId5    = "origin_id15"
  lazy final val externalId6  = "ext_id16"
  lazy final val originId6    = "origin_id16"
  lazy final val externalId7  = "ext_id17"
  lazy final val originId7    = "origin_id17"
  lazy final val externalId8  = "ext_id18"
  lazy final val originId8    = "origin_id18"
  lazy final val externalId9  = "ext_id19"
  lazy final val originId9    = "origin_id19"
  lazy final val externalId10 = "ext_id20"
  lazy final val originId10   = "origin_id20"

  lazy final val institutionSeed1  = InstitutionSeed(
    externalId = externalId1,
    originId = originId1,
    description = "Institutions Nine",
    digitalAddress = "mail9@mail.org",
    address = "address1",
    zipCode = "zipCode1",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Option(Map.empty[String, InstitutionProduct]),
    origin = "IPA",
    institutionType = Option("PA")
  )
  lazy final val institutionSeed2  = InstitutionSeed(
    externalId = externalId2,
    originId = originId2,
    description = "Institutions Ten",
    digitalAddress = "mail10@mail.org",
    address = "address2",
    zipCode = "zipCode2",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Option(Map.empty[String, InstitutionProduct]),
    origin = "IPA",
    institutionType = Option("PA")
  )
  lazy final val institutionSeed3  = InstitutionSeed(
    externalId = externalId3,
    originId = originId3,
    description = "Institutions Eleven",
    digitalAddress = "mail11@mail.org",
    address = "address3",
    zipCode = "zipCode3",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Option(Map.empty[String, InstitutionProduct]),
    origin = "IPA",
    institutionType = Option("PA")
  )
  lazy final val institutionSeed4  = InstitutionSeed(
    externalId = externalId4,
    originId = originId4,
    description = "Institutions Twelve",
    digitalAddress = "mail11@mail.org",
    address = "address4",
    zipCode = "zipCode4",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Option(Map.empty[String, InstitutionProduct]),
    origin = "IPA",
    institutionType = Option("PA")
  )
  lazy final val institutionSeed5  = InstitutionSeed(
    externalId = externalId5,
    originId = originId5,
    description = "Institutions Fifteen",
    digitalAddress = "mail15@mail.org",
    address = "address4",
    zipCode = "zipCode4",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Option(Map.empty[String, InstitutionProduct]),
    origin = "IPA",
    institutionType = Option("PA")
  )
  lazy final val institutionSeed6  = InstitutionSeed(
    externalId = externalId6,
    originId = originId6,
    description = "Institutions Sixteen",
    digitalAddress = "mail15@mail.org",
    address = "address5",
    zipCode = "zipCode5",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Option(Map.empty[String, InstitutionProduct]),
    origin = "IPA",
    institutionType = Option("PA")
  )
  lazy final val institutionSeed7  = InstitutionSeed(
    externalId = externalId7,
    originId = originId7,
    description = "Institutions Seventeen",
    digitalAddress = "mail15@mail.org",
    address = "address6",
    zipCode = "zipCode6",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Option(Map.empty[String, InstitutionProduct]),
    origin = "IPA",
    institutionType = Option("PA")
  )
  lazy final val institutionSeed8  = InstitutionSeed(
    externalId = externalId8,
    originId = originId8,
    description = "Institutions Eighteen",
    digitalAddress = "mail18@mail.org",
    address = "address8",
    zipCode = "zipCode8",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Option(
      Map(
        "p1" -> InstitutionProduct(
          product = "p1",
          pricingPlan = Option("pricingPlan"),
          billing = Billing(vatNumber = "vatNumber", recipientCode = "recipientCode", publicServices = None)
        ),
        "p2" -> InstitutionProduct(
          product = "p2",
          pricingPlan = Option("pricingPlan2"),
          billing = Billing(vatNumber = "vatNumber2", recipientCode = "recipientCode2", publicServices = Option(false))
        )
      )
    ),
    origin = "DUMMY",
    institutionType = None,
    geographicTaxonomies = Some(Seq(GeographicTaxonomy(code = "GEOCODE", desc = "GEODESC")))
  )
  lazy final val institutionSeed9  = InstitutionSeed(
    externalId = externalId9,
    originId = originId9,
    description = "Institutions Eleven",
    digitalAddress = "mail11@mail.org",
    address = "address3",
    zipCode = "zipCode3",
    taxCode = "taxCode",
    attributes = Seq.empty,
    products = Option(Map.empty[String, InstitutionProduct]),
    origin = "SELC",
    institutionType = Option("PA")
  )
  lazy final val institutionSeed10 = InstitutionSeed(
    externalId = externalId10,
    originId = originId10,
    description = "Institutions X",
    digitalAddress = "mail18@mail.org",
    address = "address10",
    zipCode = "zipCode10",
    taxCode = "taxCode10",
    attributes = Seq.empty,
    products = Option(
      Map(
        "p1" -> InstitutionProduct(
          product = "p1",
          pricingPlan = Option("pricingPlan"),
          billing = Billing(vatNumber = "vatNumber", recipientCode = "recipientCode", publicServices = None)
        ),
        "p2" -> InstitutionProduct(
          product = "p2",
          pricingPlan = Option("pricingPlan2"),
          billing = Billing(vatNumber = "vatNumber2", recipientCode = "recipientCode2", publicServices = Option(false))
        )
      )
    ),
    origin = "SELC",
    institutionType = None
  )

  // format: off
  lazy final val relationshipSeed2 = RelationshipSeed(from = personId1, to = orgId1, role = PartyRole.DELEGATE, product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed1 = RelationshipSeed(from = personId1, to = orgId1, role = PartyRole.MANAGER,  product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed3 = RelationshipSeed(from = personId2, to = orgId2, role = PartyRole.MANAGER,  product = RelationshipProductSeed(id = "p1", role ="admin"),
    institutionUpdate = Option(InstitutionUpdate(institutionType=Option("OVERRIDE_institutionType"),description=Option("OVERRIDE_description"), digitalAddress=Option("OVERRIDE_digitalAddress"), address=Option("OVERRIDE_address"), zipCode=Option("OVERRIDE_zipCode"), taxCode=Option("OVERRIDE_taxCode"), geographicTaxonomies = Seq(GeographicTaxonomy(code = "OVERRIDE_GEOCODE", desc = "OVERRIDE_GEODESC")))),
    pricingPlan = Option("OVERRIDE_pricingPlan"), billing = Option(Billing(vatNumber="OVERRIDE_vatNumber", recipientCode="OVERRIDE_recipientCode", publicServices=Option(true))))
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
  lazy final val relationshipSeed15 = RelationshipSeed(from = personId2, to = orgId8, role = PartyRole.MANAGER,  product = RelationshipProductSeed(id = "p1", role ="admin"),
    institutionUpdate = Option(InstitutionUpdate(institutionType=Option("OVERRIDE_institutionType"),description=Option("OVERRIDE_description"), digitalAddress=Option("OVERRIDE_digitalAddress"), address=Option("OVERRIDE_address"), zipCode=Option("OVERRIDE_zipCode"), taxCode=Option("OVERRIDE_taxCode"), geographicTaxonomies = Seq.empty)),
    pricingPlan = Option("OVERRIDE_pricingPlan"), billing = Option(Billing(vatNumber="OVERRIDE_vatNumber", recipientCode="OVERRIDE_recipientCode", publicServices=Option(true))))
  lazy final val relationshipSeed16 = RelationshipSeed(from = personId2, to = orgId8, role = PartyRole.DELEGATE, product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed17 = RelationshipSeed(from = personId8, to = orgId9, role = PartyRole.MANAGER,  product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed18 = RelationshipSeed(from = personId8, to = orgId9, role = PartyRole.DELEGATE,  product = RelationshipProductSeed(id = "p1", role ="admin"))
  lazy final val relationshipSeed19 = RelationshipSeed(from = personId10, to = orgId10, role = PartyRole.MANAGER, product = RelationshipProductSeed(id = "p1", role = "admin"), state = Option(RelationshipState.TOBEVALIDATED))
  lazy final val relationshipSeed20 = RelationshipSeed(from = personId10, to = orgId10, role = PartyRole.DELEGATE, product = RelationshipProductSeed(id = "p1", role = "admin"), state = Option(RelationshipState.TOBEVALIDATED))

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
  lazy final val relationship15 = Relationship(id = UUID.randomUUID(), from = personId2, to = orgId8, role = PartyRole.MANAGER, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship16 = Relationship(id = UUID.randomUUID(), from = personId2, to = orgId8, role = PartyRole.DELEGATE, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.PENDING, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship17 = Relationship(id = UUID.randomUUID(), from = personId10, to = orgId10, role = PartyRole.MANAGER, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.TOBEVALIDATED, createdAt = OffsetDateTime.now(), updatedAt = None)
  lazy final val relationship18 = Relationship(id = UUID.randomUUID(), from = personId10, to = orgId10, role = PartyRole.DELEGATE, product = RelationshipProduct(id = "p1", role ="admin", createdAt = OffsetDateTime.now()), fileName = None, contentType = None, state = RelationshipState.TOBEVALIDATED, createdAt = OffsetDateTime.now(), updatedAt = None)

  lazy final val relationshipId1 = UUID.randomUUID()
  lazy final val relationshipId2 = UUID.randomUUID()
  lazy final val relationshipId3 = UUID.randomUUID()
  lazy final val relationshipId4 = UUID.randomUUID()
  lazy final val relationshipId5 = UUID.randomUUID()
  lazy final val relationshipId6 = UUID.randomUUID()
  lazy final val relationshipId7 = UUID.randomUUID()
  lazy final val relationshipId8 = UUID.randomUUID()
  lazy final val relationshipId9 = UUID.randomUUID()
  lazy final val relationshipId10 = UUID.randomUUID()
  lazy final val relationshipId11 = UUID.randomUUID()
  lazy final val relationshipId12 = UUID.randomUUID()
  lazy final val relationshipId15 = UUID.randomUUID()
  lazy final val relationshipId16 = UUID.randomUUID()
  lazy final val relationshipId17 = UUID.randomUUID()
  lazy final val relationshipId18 = UUID.randomUUID()

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
  lazy final val partyRelationship15 = PersistedPartyRelationship(id = relationshipId15, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId2, to = orgId8, role = PersistedPartyRole.Manager,  product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None, pricingPlan = None, institutionUpdate = None, billing = None)
  lazy final val partyRelationship16 = PersistedPartyRelationship(id = relationshipId16, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId2, to = orgId8, role = PersistedPartyRole.Delegate, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now() ), filePath = None, fileName = None, contentType = None, onboardingTokenId = None, pricingPlan = None, institutionUpdate = None, billing = None)
  lazy final val partyRelationship17 = PersistedPartyRelationship(id = relationshipId17, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId10, to = orgId10, role = PersistedPartyRole.Manager, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now()), filePath = None, fileName = None, contentType = None, onboardingTokenId = None, pricingPlan = None, institutionUpdate = None, billing = None)
  lazy final val partyRelationship18 = PersistedPartyRelationship(id = relationshipId18, createdAt = OffsetDateTime.now(), updatedAt = None, state = Pending, from = personId10, to = orgId10, role = PersistedPartyRole.Delegate, product = PersistedProduct(id = "p1", role = "admin", createdAt = OffsetDateTime.now()), filePath = None, fileName = None, contentType = None, onboardingTokenId = None, pricingPlan = None, institutionUpdate = None, billing = None)

  lazy val tokenSeed1: TokenSeed = TokenSeed(id = tokenId2.toString, relationships = Relationships(Seq(relationship1, relationship2)), "checksum", OnboardingContractInfo("test", "test"))
  lazy val tokenSeed2: TokenSeed = TokenSeed(id = tokenId3.toString, relationships = Relationships(Seq(relationship3, relationship4)), "checksum", OnboardingContractInfo("test", "test"))
  lazy val tokenSeed3: TokenSeed = TokenSeed(id = tokenId4.toString, relationships = Relationships(Seq(relationship5, relationship6)), "checksum", OnboardingContractInfo("test", "test"))
  lazy val tokenSeed4: TokenSeed = TokenSeed(id = tokenId5.toString, relationships = Relationships(Seq(relationship7, relationship8)), "checksum", OnboardingContractInfo("test", "test"))
  lazy val tokenSeed5: TokenSeed = TokenSeed(id = tokenId6.toString, relationships = Relationships(Seq(relationship9, relationship10)), "checksum", OnboardingContractInfo("test", "test"))
  lazy val tokenSeed6: TokenSeed = TokenSeed(id = tokenId7.toString, relationships = Relationships(Seq(relationship11, relationship12)), "checksum", OnboardingContractInfo("test", "test"))
  lazy val tokenSeed8: TokenSeed = TokenSeed(id = tokenId8.toString, relationships = Relationships(Seq(relationship15, relationship16)), "checksum", OnboardingContractInfo("test", "test"))
  lazy val tokenSeed9: TokenSeed = TokenSeed(id = tokenId9.toString, relationships = Relationships(Seq(relationship15, relationship16)), "checksum", OnboardingContractInfo("test", "test"))
  lazy val tokenSeed10: TokenSeed = TokenSeed(id = tokenId10.toString, relationships = Relationships(Seq(relationship17, relationship18)), "checksum", OnboardingContractInfo("test", "test"))

  lazy val token1: Token = Token.generate(tokenSeed1, Seq(partyRelationship1, partyRelationship2),timestampValid).toOption.get
  lazy val token2: Token = Token.generate(tokenSeed2, Seq(partyRelationship3, partyRelationship4),timestampValid).toOption.get
  lazy val token3: Token = Token.generate(tokenSeed3, Seq(partyRelationship5, partyRelationship6),timestampValid).toOption.get
  lazy val token4: Token = Token.generate(tokenSeed4, Seq(partyRelationship7, partyRelationship8),timestampValid).toOption.get
  lazy val token5: Token = Token.generate(tokenSeed5, Seq(partyRelationship9, partyRelationship10),timestampValid).toOption.get
  lazy val token6: Token = Token.generate(tokenSeed6, Seq(partyRelationship11, partyRelationship12),timestampValid).toOption.get
  lazy val token8: Token = Token.generate(tokenSeed8, Seq(partyRelationship15, partyRelationship16),timestampValid).toOption.get
  lazy val token9: Token = Token.generate(tokenSeed9, Seq(partyRelationship15, partyRelationship16),timestampValid).toOption.get
  lazy val token10: Token = Token.generate(tokenSeed10, Seq(partyRelationship17, partyRelationship18),timestampValid).toOption.get

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

  lazy final val expected2 = Institution(
    id = orgId2,
    externalId = externalId2,
    originId = originId2,
    description = "Institutions Ten",
    digitalAddress = "mail10@mail.org",
    address = "address2",
    zipCode = "zipCode2",
    taxCode = "taxCode",
    attributes = Seq.empty,
    origin = "IPA",
    // override from relationshipSeed3
    institutionType = Option("OVERRIDE_institutionType"),
    products = Map(
      "p1" -> InstitutionProduct(
        product = "p1",
        pricingPlan = Option("OVERRIDE_pricingPlan"),
        billing = Billing(
          vatNumber = "OVERRIDE_vatNumber",
          recipientCode = "OVERRIDE_recipientCode",
          publicServices = Option(true)
        )
      )
    ),
    geographicTaxonomies = Seq(GeographicTaxonomy(code = "OVERRIDE_GEOCODE", desc = "OVERRIDE_GEODESC"))
  )

  lazy final val expected8 = Institution(
    id = orgId8,
    externalId = externalId8,
    originId = originId8,
    attributes = Seq.empty,
    origin = "DUMMY",

    // override from relationshipSeed3
    institutionType = Option("OVERRIDE_institutionType"),
    description = "OVERRIDE_description",
    digitalAddress = "OVERRIDE_digitalAddress",
    address = "OVERRIDE_address",
    zipCode = "OVERRIDE_zipCode",
    taxCode = "OVERRIDE_taxCode",
    products = Map(
      "p1" -> InstitutionProduct(
        product = "p1",
        pricingPlan = Option("OVERRIDE_pricingPlan"),
        billing = Billing(
          vatNumber = "OVERRIDE_vatNumber",
          recipientCode = "OVERRIDE_recipientCode",
          publicServices = Option(true)
        )
      ),
      "p2" -> InstitutionProduct(
        product = "p2",
        pricingPlan = Option("pricingPlan2"),
        billing = Billing(vatNumber = "vatNumber2", recipientCode = "recipientCode2", publicServices = Option(false))
      )
    ),
    geographicTaxonomies = Seq.empty
  )
}
