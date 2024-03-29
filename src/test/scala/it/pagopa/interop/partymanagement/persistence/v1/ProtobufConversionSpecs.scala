package it.pagopa.interop.partymanagement.persistence.v1

import it.pagopa.interop.commons.utils.TypeConversions.{LongOps, OffsetDateTimeOps}
import it.pagopa.interop.partymanagement.common.utils.ErrorOr
import it.pagopa.interop.partymanagement.model.party._
import it.pagopa.interop.partymanagement.model.persistence.{
  AttributesAdded,
  PartyAdded,
  PartyDeleted,
  PartyRelationshipActivated,
  PartyRelationshipAdded,
  PartyRelationshipConfirmed,
  PartyRelationshipDeleted,
  PartyRelationshipEnabled,
  PartyRelationshipRejected,
  PartyRelationshipSuspended,
  PartyUpdated,
  TokenAdded,
  TokenUpdated
}
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1._
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1.events.{
  AttributesAddedV1,
  PartyAddedV1,
  PartyDeletedV1,
  PartyRelationshipActivatedV1,
  PartyRelationshipAddedV1,
  PartyRelationshipConfirmedV1,
  PartyRelationshipDeletedV1,
  PartyRelationshipEnabledV1,
  PartyRelationshipRejectedV1,
  PartyRelationshipSuspendedV1,
  PartyUpdatedV1,
  TokenAddedV1,
  TokenUpdatedV1
}
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1.party.{
  InstitutionAttributeV1,
  InstitutionPartyV1,
  InstitutionProductV1,
  PartyV1,
  PersonPartyV1
}
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1.relationship.{
  BillingV1,
  DataProtectionOfficerV1,
  GeographicTaxonomyV1,
  InstitutionUpdateV1,
  PartyRelationshipProductV1,
  PartyRelationshipV1,
  PaymentServiceProviderV1
}
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1.token.{
  OnboardingContractInfoV1,
  PartyRelationshipBindingV1,
  TokenV1
}
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1.utils._
import it.pagopa.interop.partymanagement.model.persistence.serializer.{PersistEventDeserializer, PersistEventSerializer}
import org.scalatest.EitherValues._
import org.scalatest.TryValues._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
import java.util.UUID
import scala.util.Try

class ProtobufConversionSpecs extends AnyWordSpecLike with Matchers {

  "Protobuf conversions" should {

    "convert a PartyV1 (PersonPartyV1) to Party (PersonParty)" in {
      val id                          = UUID.randomUUID()
      val start                       = OffsetDateTime.now()
      val end                         = OffsetDateTime.now().plusDays(10L)
      val partyV1: Try[PersonPartyV1] =
        for {
          start <- start.asFormattedString
          end   <- end.asFormattedString
        } yield PersonPartyV1(id = id.toString, start = start, end = Some(end))

      val party: Either[Throwable, Party] = partyV1.toEither.flatMap(getParty)

      val expected: PersonParty = PersonParty(id = id, start = start, end = Some(end))

      party.value shouldBe expected

    }

    "convert a PartyV1 (InstitutionPartyV1) to Party (InstitutionParty)" in {
      val id                     = UUID.randomUUID()
      val externalId             = "externalId"
      val originId               = "originId"
      val description            = "description"
      val digitalAddress         = "digitalAddress"
      val address                = "address"
      val zipCode                = "zipCode"
      val taxCode                = "taxCode"
      val start                  = OffsetDateTime.now()
      val end                    = OffsetDateTime.now().plusDays(10L)
      val origin                 = "IPA"
      val institutionType        = "PA"
      val attributes             = Set(
        InstitutionAttribute(origin = "origin", code = "a", description = "description_a"),
        InstitutionAttribute(origin = "origin", code = "b", description = "description_b")
      )
      val products               = Set(
        PersistedInstitutionProduct(
          product = "product1",
          pricingPlan = Option("pricingPlan"),
          billing = PersistedBilling(vatNumber = "VATNUMBER", recipientCode = "RECIPIENTCODE", publicServices = None)
        ),
        PersistedInstitutionProduct(
          product = "product1",
          pricingPlan = Option("pricingPlan"),
          billing =
            PersistedBilling(vatNumber = "VATNUMBER", recipientCode = "RECIPIENTCODE", publicServices = Option(true))
        )
      )
      val paymentServiceProvider = PersistedPaymentServiceProvider(
        abiCode = Some("12345"),
        businessRegisterNumber = Some("123456"),
        legalRegisterName = Some("Register Name"),
        legalRegisterNumber = Some("1234567"),
        vatNumberGroup = Some(false)
      )
      val dataProtectionOfficer  =
        PersistedDataProtectionOfficer(
          address = Some("via Roma 1"),
          email = Some("ciao@ciao.it"),
          pec = Some("pec@pec.it")
        )

      val geographicTaxonomies = Seq(
        PersistedGeographicTaxonomy(code = "GEOCODE", desc = "GEODESC"),
        PersistedGeographicTaxonomy(code = "GEOCODE2", desc = "GEODESC2")
      )

      val partyV1: Try[InstitutionPartyV1] =
        for {
          start <- start.asFormattedString
          end   <- end.asFormattedString
        } yield InstitutionPartyV1(
          id = id.toString,
          externalId = externalId,
          originId = originId,
          description = description,
          digitalAddress = digitalAddress,
          address = address,
          zipCode = zipCode,
          taxCode = taxCode,
          start = start,
          end = Some(end),
          origin = origin,
          institutionType = Option(institutionType),
          products = products.toSeq.map(p =>
            InstitutionProductV1(
              product = p.product,
              billing = BillingV1(p.billing.vatNumber, p.billing.recipientCode, p.billing.publicServices),
              pricingPlan = p.pricingPlan
            )
          ),
          attributes = attributes.toSeq.map(attribute =>
            InstitutionAttributeV1(
              origin = attribute.origin,
              code = attribute.code,
              description = attribute.description
            )
          ),
          paymentServiceProvider = Option(
            PaymentServiceProviderV1(
              abiCode = paymentServiceProvider.abiCode,
              businessRegisterNumber = paymentServiceProvider.businessRegisterNumber,
              legalRegisterName = paymentServiceProvider.legalRegisterName,
              legalRegisterNumber = paymentServiceProvider.legalRegisterNumber,
              vatNumberGroup = paymentServiceProvider.vatNumberGroup
            )
          ),
          dataProtectionOfficer = Option(
            DataProtectionOfficerV1(
              address = dataProtectionOfficer.address,
              email = dataProtectionOfficer.email,
              pec = dataProtectionOfficer.pec
            )
          ),
          geographicTaxonomies = geographicTaxonomies.map(x => GeographicTaxonomyV1(code = x.code, desc = x.desc)),
          rea = None,
          shareCapital = None,
          businessRegisterPlace = None,
          supportEmail = None,
          supportPhone = None,
          imported = None
        )

      val party: Either[Throwable, Party] = partyV1.toEither.flatMap(getParty)

      val expected: InstitutionParty = InstitutionParty(
        id = id,
        externalId = externalId,
        originId = originId,
        description = description,
        digitalAddress = digitalAddress,
        address = address,
        zipCode = zipCode,
        taxCode = taxCode,
        start = start,
        end = Some(end),
        origin = origin,
        institutionType = Option(institutionType),
        products = products,
        attributes = attributes,
        paymentServiceProvider = Option(paymentServiceProvider),
        dataProtectionOfficer = Option(dataProtectionOfficer),
        geographicTaxonomies = geographicTaxonomies,
        rea = None,
        shareCapital = None,
        businessRegisterPlace = None,
        supportEmail = None,
        supportPhone = None,
        imported = None
      )

      party.value shouldBe expected

    }

    "convert a Party (PersonParty) to PartyV1 (PersonPartyV1)" in {
      val id                 = UUID.randomUUID()
      val start              = OffsetDateTime.now()
      val end                = OffsetDateTime.now().plusDays(10L)
      val party: PersonParty = PersonParty(id = id, start = start, end = Some(end))

      val partyV1: ErrorOr[PartyV1] = getPartyV1(party)

      val expected: Try[PersonPartyV1] =
        for {
          start <- start.asFormattedString
          end   <- end.asFormattedString
        } yield PersonPartyV1(id = id.toString, start = start, end = Some(end))

      partyV1.value shouldBe expected.success.value

    }

    "convert a Party (InstitutionParty) to PartyV1 (InstitutionPartyV1)" in {
      val id                   = UUID.randomUUID()
      val externalId           = "externalId"
      val originId             = "originId"
      val description          = "description"
      val digitalAddress       = "digitalAddress"
      val address              = "address"
      val zipCode              = "zipCode"
      val taxCode              = "taxCode"
      val start                = OffsetDateTime.now()
      val end                  = OffsetDateTime.now().plusDays(10L)
      val origin               = "IPA"
      val institutionType      = "PA"
      val attributes           =
        Seq(
          InstitutionAttributeV1(origin = "origin", code = "a", description = "description_a"),
          InstitutionAttributeV1(origin = "origin", code = "b", description = "description_b")
        )
      val products             = Seq(
        InstitutionProductV1(
          product = "product1",
          pricingPlan = Option("pricingPlan"),
          billing = BillingV1(vatNumber = "VATNUMBER", recipientCode = "RECIPIENTCODE", publicServices = None)
        ),
        InstitutionProductV1(
          product = "product1",
          pricingPlan = Option("pricingPlan"),
          billing = BillingV1(vatNumber = "VATNUMBER", recipientCode = "RECIPIENTCODE", publicServices = Option(true))
        )
      )
      val geographicTaxonomies = Seq(
        PersistedGeographicTaxonomy(code = "GEOCODE", desc = "GEODESC"),
        PersistedGeographicTaxonomy(code = "GEOCODE2", desc = "GEODESC2")
      )

      val party: InstitutionParty = InstitutionParty(
        id = id,
        externalId = externalId,
        originId = originId,
        description = description,
        digitalAddress = digitalAddress,
        address = address,
        zipCode = zipCode,
        taxCode = taxCode,
        start = start,
        end = Some(end),
        origin = origin,
        institutionType = Option(institutionType),
        products = products
          .map(p =>
            PersistedInstitutionProduct(
              product = p.product,
              billing = PersistedBilling(p.billing.vatNumber, p.billing.recipientCode, p.billing.publicServices),
              pricingPlan = p.pricingPlan
            )
          )
          .toSet,
        attributes = attributes
          .map(attr => InstitutionAttribute(origin = attr.origin, code = attr.code, description = attr.description))
          .toSet,
        paymentServiceProvider = None,
        dataProtectionOfficer = None,
        geographicTaxonomies = geographicTaxonomies,
        rea = None,
        shareCapital = None,
        businessRegisterPlace = None,
        supportEmail = None,
        supportPhone = None,
        imported = None
      )

      val partyV1: Either[Throwable, PartyV1] = getPartyV1(party)

      val expected: Try[InstitutionPartyV1] =
        for {
          start <- start.asFormattedString
          end   <- end.asFormattedString
        } yield InstitutionPartyV1(
          id = id.toString,
          externalId = externalId,
          originId = originId,
          description = description,
          digitalAddress = digitalAddress,
          address = address,
          zipCode = zipCode,
          taxCode = taxCode,
          start = start,
          end = Some(end),
          origin = origin,
          institutionType = Option(institutionType),
          products = products,
          attributes = attributes,
          paymentServiceProvider = None,
          dataProtectionOfficer = None,
          geographicTaxonomies = geographicTaxonomies.map(x => GeographicTaxonomyV1(code = x.code, desc = x.desc)),
          rea = None,
          shareCapital = None,
          businessRegisterPlace = None,
          supportEmail = None,
          supportPhone = None,
          imported = None
        )

      partyV1.value shouldBe expected.success.value

    }

    /* convert a PartyRelationshipV1 to PartyRelationship */

    "convert a PartyRelationshipV1 to PartyRelationship" in {

      val relationshipId    = UUID.randomUUID()
      val from              = UUID.randomUUID()
      val to                = UUID.randomUUID()
      val productId         = "productId"
      val productRole       = "productRole"
      val productCreatedAt  = OffsetDateTime.now()
      val filePath          = Some("path")
      val fileName          = Some("fileName")
      val contentType       = Some("contentType")
      val onboardingTokenId = UUID.randomUUID()
      val pricingPlan       = Option("PRICING_PLAN")
      val billing           = Option(BillingV1("VATNUMBER", "RECIPIENTCODE", Option(true)))
      val institutionUpdate = Option(
        InstitutionUpdateV1(
          Option("PAOVERRIDE"),
          Option("DESCRIPTIONOVERRIDE"),
          Option("MAILOVERRIDE"),
          Option("ADDRESSOVERRIDE"),
          Option("ZIPCODEOVERRIDE"),
          Option("TAXCODEOVERRIDE"),
          paymentServiceProvider = None,
          dataProtectionOfficer = None,
          geographicTaxonomies = Seq(
            GeographicTaxonomyV1(code = "GEOCODE", desc = "GEODESC"),
            GeographicTaxonomyV1(code = "GEOCODE2", desc = "GEODESC2")
          ),
          rea = None,
          shareCapital = None,
          businessRegisterPlace = None,
          supportEmail = None,
          supportPhone = None,
          imported = None
        )
      )
      val createdAt         = OffsetDateTime.now()
      val updatedAt         = OffsetDateTime.now().plusDays(10L)

      val partyRelationshipV1: PartyRelationshipV1 = PartyRelationshipV1(
        id = relationshipId.toString,
        from = from.toString,
        to = to.toString,
        role = PartyRelationshipV1.PartyRoleV1.MANAGER,
        product = PartyRelationshipProductV1(id = productId, role = productRole, createdAt = productCreatedAt.toMillis),
        createdAt = createdAt.toMillis,
        updatedAt = Some(updatedAt.toMillis),
        state = PartyRelationshipV1.PartyRelationshipStateV1.ACTIVE,
        filePath = filePath,
        fileName = fileName,
        contentType = contentType,
        onboardingTokenId = Some(onboardingTokenId.toString),
        pricingPlan = pricingPlan,
        institutionUpdate = institutionUpdate,
        billing = billing
      )

      val partyRelationship: Either[Throwable, PersistedPartyRelationship] = getPartyRelationship(partyRelationshipV1)

      val expected: Try[PersistedPartyRelationship] = for {
        c  <- createdAt.toMillis.toOffsetDateTime
        u  <- updatedAt.toMillis.toOffsetDateTime
        pc <- productCreatedAt.toMillis.toOffsetDateTime
      } yield PersistedPartyRelationship(
        id = relationshipId,
        from = from,
        to = to,
        role = PersistedPartyRole.Manager,
        product = PersistedProduct(id = productId, role = productRole, createdAt = pc),
        state = PersistedPartyRelationshipState.Active,
        filePath = filePath,
        fileName = fileName,
        contentType = contentType,
        onboardingTokenId = Some(onboardingTokenId),
        createdAt = c,
        updatedAt = Some(u),
        pricingPlan = pricingPlan,
        institutionUpdate = institutionUpdate.map(i =>
          PersistedInstitutionUpdate(
            i.institutionType,
            i.description,
            i.digitalAddress,
            i.address,
            i.zipCode,
            i.taxCode,
            i.paymentServiceProvider.map(p =>
              PersistedPaymentServiceProvider(
                abiCode = p.abiCode,
                businessRegisterNumber = p.businessRegisterNumber,
                legalRegisterName = p.legalRegisterName,
                legalRegisterNumber = p.legalRegisterNumber,
                vatNumberGroup = p.vatNumberGroup
              )
            ),
            i.dataProtectionOfficer
              .map(d => PersistedDataProtectionOfficer(address = d.address, email = d.email, pec = d.pec)),
            institutionUpdate.get.geographicTaxonomies
              .map(x => PersistedGeographicTaxonomy(code = x.code, desc = x.desc)),
            i.rea,
            i.shareCapital,
            i.businessRegisterPlace,
            i.supportEmail,
            i.supportPhone,
            i.imported
          )
        ),
        billing = billing.map(b => PersistedBilling(b.vatNumber, b.recipientCode, b.publicServices))
      )

      partyRelationship.value shouldBe expected.success.value

    }

    /* convert a PartyRelationship to PartyRelationshipV1 */

    "convert a PartyRelationship to PartyRelationshipV1" in {

      val relationshipId    = UUID.randomUUID()
      val from              = UUID.randomUUID()
      val to                = UUID.randomUUID()
      val productId         = "productId"
      val productRole       = "productRole"
      val productCreatedAt  = OffsetDateTime.now()
      val filePath          = Some("path")
      val fileName          = Some("fileName")
      val contentType       = Some("contentType")
      val onboardingTokenId = UUID.randomUUID()
      val pricingPlan       = Option("PRICING_PLAN")
      val billing           = Option(BillingV1("VATNUMBER", "RECIPIENTCODE", Option(true)))
      val institutionUpdate = Option(
        InstitutionUpdateV1(
          Option("PAOVERRIDE"),
          Option("DESCRIPTIONOVERRIDE"),
          Option("MAILOVERRIDE"),
          Option("ADDRESSOVERRIDE"),
          Option("ZIPCODEOVERRIDE"),
          Option("TAXCODEOVERRIDE"),
          paymentServiceProvider = None,
          dataProtectionOfficer = None,
          geographicTaxonomies = Seq(
            GeographicTaxonomyV1(code = "GEOCODE", desc = "GEODESC"),
            GeographicTaxonomyV1(code = "GEOCODE2", desc = "GEODESC2")
          ),
          rea = None,
          shareCapital = None,
          businessRegisterPlace = None,
          supportEmail = None,
          supportPhone = None,
          imported = None
        )
      )
      val createdAt         = OffsetDateTime.now()
      val updatedAt         = OffsetDateTime.now().plusDays(10L)

      val persistedPartyRelationship: Try[PersistedPartyRelationship] =
        for {
          c  <- createdAt.toMillis.toOffsetDateTime
          u  <- updatedAt.toMillis.toOffsetDateTime
          pc <- productCreatedAt.toMillis.toOffsetDateTime
        } yield PersistedPartyRelationship(
          id = relationshipId,
          from = from,
          to = to,
          role = PersistedPartyRole.Manager,
          product = PersistedProduct(id = productId, role = productRole, createdAt = pc),
          state = PersistedPartyRelationshipState.Active,
          filePath = filePath,
          fileName = fileName,
          contentType = contentType,
          onboardingTokenId = Some(onboardingTokenId),
          createdAt = c,
          updatedAt = Some(u),
          pricingPlan = pricingPlan,
          institutionUpdate = institutionUpdate.map(i =>
            PersistedInstitutionUpdate(
              i.institutionType,
              i.description,
              i.digitalAddress,
              i.address,
              i.zipCode,
              i.taxCode,
              i.paymentServiceProvider.map(p =>
                PersistedPaymentServiceProvider(
                  abiCode = p.abiCode,
                  businessRegisterNumber = p.businessRegisterNumber,
                  legalRegisterName = p.legalRegisterName,
                  legalRegisterNumber = p.legalRegisterNumber,
                  vatNumberGroup = p.vatNumberGroup
                )
              ),
              i.dataProtectionOfficer.map(d =>
                PersistedDataProtectionOfficer(address = d.address, email = d.email, pec = d.pec)
              ),
              institutionUpdate.get.geographicTaxonomies.map(x =>
                PersistedGeographicTaxonomy(code = x.code, desc = x.desc)
              ),
              rea = None,
              shareCapital = None,
              businessRegisterPlace = None,
              supportEmail = None,
              supportPhone = None,
              imported = None
            )
          ),
          billing = billing.map(b => PersistedBilling(b.vatNumber, b.recipientCode, b.publicServices))
        )

      val partyRelationshipV1: Try[PartyRelationshipV1] = persistedPartyRelationship.map(getPartyRelationshipV1)

      val expected: PartyRelationshipV1 = PartyRelationshipV1(
        id = relationshipId.toString,
        from = from.toString,
        to = to.toString,
        role = PartyRelationshipV1.PartyRoleV1.MANAGER,
        product = PartyRelationshipProductV1(id = productId, role = productRole, createdAt = productCreatedAt.toMillis),
        createdAt = createdAt.toMillis,
        updatedAt = Some(updatedAt.toMillis),
        state = PartyRelationshipV1.PartyRelationshipStateV1.ACTIVE,
        filePath = filePath,
        fileName = fileName,
        contentType = contentType,
        onboardingTokenId = Some(onboardingTokenId.toString),
        pricingPlan = pricingPlan,
        billing = billing,
        institutionUpdate = institutionUpdate
      )

      partyRelationshipV1.success.value shouldBe expected

    }

    /* convert a TokenV1 to Token */
    "convert a TokenV1 to Token" in {

      val id                                     = UUID.randomUUID()
      val partyId                                = UUID.randomUUID()
      val relationshipId                         = UUID.randomUUID()
      val validity: OffsetDateTime               = OffsetDateTime.now()
      val checksum: String                       = "checksum"
      val path: String                           = "path"
      val version: String                        = "version"
      val contractInfo: OnboardingContractInfoV1 = OnboardingContractInfoV1(path = path, version = version)

      val tokenV1: Try[TokenV1] = validity.asFormattedString.map(v =>
        TokenV1(
          id = id.toString,
          legals =
            Seq(PartyRelationshipBindingV1(partyId = partyId.toString, relationshipId = relationshipId.toString)),
          validity = v,
          checksum = checksum,
          contractInfo = contractInfo
        )
      )

      val token: Either[Throwable, Token] = tokenV1.toEither.flatMap(getToken)

      val expected = Token(
        id = id,
        checksum = checksum,
        legals = Seq(PartyRelationshipBinding(partyId = partyId, relationshipId = relationshipId)),
        validity = validity,
        contractInfo = TokenOnboardingContractInfo(path = path, version = version)
      )

      token.value shouldBe expected

    }

    "convert a Token to TokenV1" in {

      val id                                     = UUID.randomUUID()
      val partyId                                = UUID.randomUUID()
      val relationshipId                         = UUID.randomUUID()
      val validity: OffsetDateTime               = OffsetDateTime.now()
      val checksum: String                       = "checksum"
      val path: String                           = "path"
      val version: String                        = "version"
      val contractInfo: OnboardingContractInfoV1 = OnboardingContractInfoV1(path = path, version = version)

      val token = Token(
        id = id,
        checksum = checksum,
        legals = Seq(PartyRelationshipBinding(partyId = partyId, relationshipId = relationshipId)),
        validity = validity,
        contractInfo = TokenOnboardingContractInfo(path = path, version = version)
      )

      val tokenV1: ErrorOr[TokenV1] = getTokenV1(token)

      val expected: Try[TokenV1] = validity.asFormattedString.map(v =>
        TokenV1(
          id = id.toString,
          legals =
            Seq(PartyRelationshipBindingV1(partyId = partyId.toString, relationshipId = relationshipId.toString)),
          validity = v,
          checksum = checksum,
          contractInfo = contractInfo
        )
      )

      tokenV1.value shouldBe expected.success.value

    }

    "convert a PartyRoleV1 to PersistedPartyRole" in {

      partyRoleFromProtobuf(PartyRelationshipV1.PartyRoleV1.MANAGER).value shouldBe PersistedPartyRole.Manager
      partyRoleFromProtobuf(PartyRelationshipV1.PartyRoleV1.DELEGATE).value shouldBe PersistedPartyRole.Delegate
      partyRoleFromProtobuf(PartyRelationshipV1.PartyRoleV1.SUB_DELEGATE).value shouldBe PersistedPartyRole.SubDelegate
      partyRoleFromProtobuf(PartyRelationshipV1.PartyRoleV1.OPERATOR).value shouldBe PersistedPartyRole.Operator

    }

    "convert a PersistedPartyRole to PartyRoleV1" in {

      partyRoleToProtobuf(PersistedPartyRole.Manager) shouldBe PartyRelationshipV1.PartyRoleV1.MANAGER
      partyRoleToProtobuf(PersistedPartyRole.Delegate) shouldBe PartyRelationshipV1.PartyRoleV1.DELEGATE
      partyRoleToProtobuf(PersistedPartyRole.SubDelegate) shouldBe PartyRelationshipV1.PartyRoleV1.SUB_DELEGATE
      partyRoleToProtobuf(PersistedPartyRole.Operator) shouldBe PartyRelationshipV1.PartyRoleV1.OPERATOR

    }

    "convert a PartyRelationshipStateV1 to PersistedPartyRelationshipState" in {

      relationshipStateFromProtobuf(
        PartyRelationshipV1.PartyRelationshipStateV1.PENDING
      ).value shouldBe PersistedPartyRelationshipState.Pending

      relationshipStateFromProtobuf(
        PartyRelationshipV1.PartyRelationshipStateV1.ACTIVE
      ).value shouldBe PersistedPartyRelationshipState.Active

      relationshipStateFromProtobuf(
        PartyRelationshipV1.PartyRelationshipStateV1.SUSPENDED
      ).value shouldBe PersistedPartyRelationshipState.Suspended

      relationshipStateFromProtobuf(
        PartyRelationshipV1.PartyRelationshipStateV1.DELETED
      ).value shouldBe PersistedPartyRelationshipState.Deleted

      relationshipStateFromProtobuf(
        PartyRelationshipV1.PartyRelationshipStateV1.REJECTED
      ).value shouldBe PersistedPartyRelationshipState.Rejected

    }

    "convert a PersistedPartyRelationshipState to PartyRelationshipStateV1" in {

      relationshipStateToProtobuf(
        PersistedPartyRelationshipState.Pending
      ) shouldBe PartyRelationshipV1.PartyRelationshipStateV1.PENDING

      relationshipStateToProtobuf(
        PersistedPartyRelationshipState.Active
      ) shouldBe PartyRelationshipV1.PartyRelationshipStateV1.ACTIVE

      relationshipStateToProtobuf(
        PersistedPartyRelationshipState.Suspended
      ) shouldBe PartyRelationshipV1.PartyRelationshipStateV1.SUSPENDED

      relationshipStateToProtobuf(
        PersistedPartyRelationshipState.Rejected
      ) shouldBe PartyRelationshipV1.PartyRelationshipStateV1.REJECTED

      relationshipStateToProtobuf(
        PersistedPartyRelationshipState.Deleted
      ) shouldBe PartyRelationshipV1.PartyRelationshipStateV1.DELETED

    }

    "deserialize StateV1" in {

      val result = PersistEventDeserializer.from(StateV1Data.stateV1)

      result.value.tokens shouldBe StateData.state.tokens
      result.value.parties shouldBe StateData.state.parties
      result.value.relationships shouldBe StateData.state.relationships

    }

    "serialize State" in {

      val result = PersistEventSerializer.to(StateData.state)

      result.value.tokens.sortBy(_.key) shouldBe StateV1Data.stateV1.tokens.sortBy(_.key)
      result.value.parties.sortBy(_.key) shouldBe StateV1Data.stateV1.parties.sortBy(_.key)
      result.value.relationships.sortBy(_.key) shouldBe StateV1Data.stateV1.relationships.sortBy(_.key)
    }

    "deserialize PartyAddedV1" in {

      val result = PersistEventDeserializer.from(PartyAddedV1(party = StateV1Data.personPartyV1))

      result.value.party shouldBe StateData.personParty

    }

    "deserialize PartyUpdatedV1" in {

      val result = PersistEventDeserializer.from(PartyUpdatedV1(party = StateV1Data.personPartyV1))

      result.value.party shouldBe StateData.personParty

    }

    "serialize PartyAdded" in {

      val result = PersistEventSerializer.to(PartyAdded(party = StateData.personParty))

      result.value.party shouldBe StateV1Data.personPartyV1
    }

    "serialize PartyUpdated" in {

      val result = PersistEventSerializer.to(PartyUpdated(party = StateData.personParty))

      result.value.party shouldBe StateV1Data.personPartyV1
    }

    "deserialize PartyDeletedV1" in {

      val result = PersistEventDeserializer.from(PartyDeletedV1(party = StateV1Data.personPartyV1))

      result.value.party shouldBe StateData.personParty

    }

    "serialize PartyDeleted" in {

      val result = PersistEventSerializer.to(PartyDeleted(party = StateData.personParty))

      result.value.party shouldBe StateV1Data.personPartyV1
    }

    "deserialize AttributesAddedV1" in {

      val result = PersistEventDeserializer.from(AttributesAddedV1(party = StateV1Data.personPartyV1))

      result.value.party shouldBe StateData.personParty

    }

    "serialize AttributesAdded" in {

      val result = PersistEventSerializer.to(AttributesAdded(party = StateData.personParty))

      result.value.party shouldBe StateV1Data.personPartyV1
    }

    "deserialize PartyRelationshipAddedV1" in {

      val result =
        PersistEventDeserializer.from(PartyRelationshipAddedV1(partyRelationship = StateV1Data.relationshipV1))

      result.value.partyRelationship shouldBe StateData.relationship

    }

    "serialize PartyRelationshipAdded" in {

      val result = PersistEventSerializer.to(PartyRelationshipAdded(partyRelationship = StateData.relationship))

      result.value.partyRelationship shouldBe StateV1Data.relationshipV1
    }

    "deserialize PartyRelationshipConfirmedV1" in {

      val result =
        PersistEventDeserializer.from(
          PartyRelationshipConfirmedV1(
            partyRelationshipId = StateCommonData.relationshipId.toString,
            filePath = StateCommonData.filePath,
            fileName = StateCommonData.filePath,
            contentType = StateCommonData.contentType,
            timestamp = StateCommonData.start.toMillis,
            onboardingTokenId = StateCommonData.onboardingTokenId.toString
          )
        )

      result.value shouldBe PartyRelationshipConfirmed(
        partyRelationshipId = StateCommonData.relationshipId,
        filePath = StateCommonData.filePath,
        fileName = StateCommonData.filePath,
        contentType = StateCommonData.contentType,
        timestamp = StateCommonData.start.toMillis.toOffsetDateTime.success.value,
        onboardingTokenId = StateCommonData.onboardingTokenId
      )

    }

    "serialize PartyRelationshipConfirmed" in {

      val result =
        PersistEventSerializer.to(
          PartyRelationshipConfirmed(
            partyRelationshipId = StateCommonData.relationshipId,
            filePath = StateCommonData.filePath,
            fileName = StateCommonData.filePath,
            contentType = StateCommonData.contentType,
            timestamp = StateCommonData.start.toMillis.toOffsetDateTime.success.value,
            onboardingTokenId = StateCommonData.onboardingTokenId
          )
        )

      result.value shouldBe PartyRelationshipConfirmedV1(
        partyRelationshipId = StateCommonData.relationshipId.toString,
        filePath = StateCommonData.filePath,
        fileName = StateCommonData.filePath,
        contentType = StateCommonData.contentType,
        timestamp = StateCommonData.start.toMillis,
        onboardingTokenId = StateCommonData.onboardingTokenId.toString
      )
    }

    "deserialize PartyRelationshipRejectedV1" in {

      val result =
        PersistEventDeserializer.from(
          PartyRelationshipRejectedV1(partyRelationshipId = StateCommonData.relationshipId.toString)
        )

      result.value shouldBe PartyRelationshipRejected(partyRelationshipId = StateCommonData.relationshipId)

    }

    "serialize PartyRelationshipRejected" in {

      val result =
        PersistEventSerializer.to(PartyRelationshipRejected(partyRelationshipId = StateCommonData.relationshipId))

      result.value shouldBe PartyRelationshipRejectedV1(partyRelationshipId = StateCommonData.relationshipId.toString)
    }

    "deserialize PartyRelationshipDeletedV1" in {

      val result =
        PersistEventDeserializer.from(
          PartyRelationshipDeletedV1(
            partyRelationshipId = StateCommonData.relationshipId.toString,
            timestamp = StateCommonData.timestamp.toMillis
          )
        )

      result.value shouldBe PartyRelationshipDeleted(
        partyRelationshipId = StateCommonData.relationshipId,
        timestamp = StateCommonData.timestamp.toMillis.toOffsetDateTime.success.value
      )

    }

    "serialize PartyRelationshipDeleted" in {

      val result = PersistEventSerializer.to(
        PartyRelationshipDeleted(
          partyRelationshipId = StateCommonData.relationshipId,
          timestamp = StateCommonData.timestamp.toMillis.toOffsetDateTime.success.value
        )
      )

      result.value shouldBe PartyRelationshipDeletedV1(
        partyRelationshipId = StateCommonData.relationshipId.toString,
        timestamp = StateCommonData.timestamp.toMillis
      )
    }

    "deserialize PartyRelationshipSuspendedV1" in {

      val result =
        PersistEventDeserializer.from(
          PartyRelationshipSuspendedV1(
            partyRelationshipId = StateCommonData.relationshipId.toString,
            timestamp = StateCommonData.timestamp.toMillis
          )
        )

      result.value shouldBe PartyRelationshipSuspended(
        partyRelationshipId = StateCommonData.relationshipId,
        timestamp = StateCommonData.timestamp.toMillis.toOffsetDateTime.success.value
      )

    }

    "serialize PartyRelationshipSuspended" in {

      val result = PersistEventSerializer.to(
        PartyRelationshipSuspended(
          partyRelationshipId = StateCommonData.relationshipId,
          timestamp = StateCommonData.timestamp.toMillis.toOffsetDateTime.success.value
        )
      )

      result.value shouldBe PartyRelationshipSuspendedV1(
        partyRelationshipId = StateCommonData.relationshipId.toString,
        timestamp = StateCommonData.timestamp.toMillis
      )
    }

    "deserialize PartyRelationshipActivatedV1" in {

      val result =
        PersistEventDeserializer.from(
          PartyRelationshipActivatedV1(
            partyRelationshipId = StateCommonData.relationshipId.toString,
            timestamp = StateCommonData.timestamp.toMillis
          )
        )

      result.value shouldBe PartyRelationshipActivated(
        partyRelationshipId = StateCommonData.relationshipId,
        timestamp = StateCommonData.timestamp.toMillis.toOffsetDateTime.success.value
      )

    }

    "serialize PartyRelationshipActivated" in {

      val result = PersistEventSerializer.to(
        PartyRelationshipActivated(
          partyRelationshipId = StateCommonData.relationshipId,
          timestamp = StateCommonData.timestamp.toMillis.toOffsetDateTime.success.value
        )
      )

      result.value shouldBe PartyRelationshipActivatedV1(
        partyRelationshipId = StateCommonData.relationshipId.toString,
        timestamp = StateCommonData.timestamp.toMillis
      )
    }

    "deserialize PartyRelationshipEnabledV1" in {

      val result =
        PersistEventDeserializer.from(
          PartyRelationshipEnabledV1(
            partyRelationshipId = StateCommonData.relationshipId.toString,
            timestamp = StateCommonData.timestamp.toMillis
          )
        )

      result.value shouldBe PartyRelationshipEnabled(
        partyRelationshipId = StateCommonData.relationshipId,
        timestamp = StateCommonData.timestamp.toMillis.toOffsetDateTime.success.value
      )

    }

    "serialize PartyRelationshipEnabled" in {

      val result = PersistEventSerializer.to(
        PartyRelationshipEnabled(
          partyRelationshipId = StateCommonData.relationshipId,
          timestamp = StateCommonData.timestamp.toMillis.toOffsetDateTime.success.value
        )
      )

      result.value shouldBe PartyRelationshipEnabledV1(
        partyRelationshipId = StateCommonData.relationshipId.toString,
        timestamp = StateCommonData.timestamp.toMillis
      )
    }

    "deserialize TokenAddedV1" in {

      val result =
        PersistEventDeserializer.from(TokenAddedV1(StateV1Data.tokenV1))

      result.value shouldBe TokenAdded(StateData.token)

    }

    "serialize TokenAdded" in {

      val result =
        PersistEventSerializer.to(TokenAdded(StateData.token))

      result.value shouldBe TokenAddedV1(StateV1Data.tokenV1)
    }

    "deserialize TokenUpdatedV1" in {

      val result =
        PersistEventDeserializer.from(TokenUpdatedV1(StateV1Data.tokenV1))

      result.value shouldBe TokenUpdated(StateData.token)

    }

    "serialize TokenUpdated" in {

      val result =
        PersistEventSerializer.to(TokenUpdated(StateData.token))

      result.value shouldBe TokenUpdatedV1(StateV1Data.tokenV1)
    }

  }

}
