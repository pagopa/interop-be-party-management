package it.pagopa.interop.partymanagement.persistence.v1

import it.pagopa.interop.commons.utils.TypeConversions.OffsetDateTimeOps
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1.party.{
  InstitutionAttributeV1,
  InstitutionPartyV1,
  InstitutionProductV1,
  PersonPartyV1
}
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1.relationship.{
  BillingV1,
  DataProtectionOfficerV1,
  InstitutionUpdateV1,
  PartyRelationshipProductV1,
  PartyRelationshipV1,
  PaymentServiceProviderV1
}
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1.state.{
  PartiesV1,
  RelationshipEntryV1,
  StateV1,
  TokensV1
}
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1.token.{
  OnboardingContractInfoV1,
  PartyRelationshipBindingV1,
  TokenV1
}
import org.scalatest.TryValues._

import java.util.UUID

object StateV1Data {

  import StateCommonData._

  val productV1: PartyRelationshipProductV1 =
    PartyRelationshipProductV1(id = productId, role = productRole, createdAt = productCreatedAt.toMillis)

  val institutionUpdateV1: Option[InstitutionUpdateV1] = institutionUpdate.map(i =>
    InstitutionUpdateV1(
      institutionType = i.institutionType,
      description = i.description,
      digitalAddress = i.digitalAddress,
      address = i.address,
      zipCode = i.zipCode,
      taxCode = i.taxCode,
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
      )
    )
  )

  val billingV1: Option[BillingV1] = billing.map(b =>
    BillingV1(vatNumber = b.vatNumber, recipientCode = b.recipientCode, publicServices = b.publicServices)
  )

  def createPartyRelationshipV1(
    partyRelationshipId: UUID,
    role: PartyRelationshipV1.PartyRoleV1,
    state: PartyRelationshipV1.PartyRelationshipStateV1
  ): PartyRelationshipV1 = PartyRelationshipV1(
    id = partyRelationshipId.toString,
    from = personPartyId.toString,
    to = institutionPartyId.toString,
    role = role,
    product = productV1,
    createdAt = createdAt.toMillis,
    updatedAt = Some(updatedAt.toMillis),
    state = state,
    filePath = Some(filePath),
    fileName = Some(fileName),
    contentType = Some(contentType),
    onboardingTokenId = Some(onboardingTokenId.toString),
    pricingPlan = pricingPlan,
    institutionUpdate = institutionUpdateV1,
    billing = billingV1
  )

  val personPartyV1: PersonPartyV1 = PersonPartyV1(
    id = personPartyId.toString,
    start = start.asFormattedString.success.value,
    end = Some(end.asFormattedString.success.value)
  )

  val institutionPartyV1: InstitutionPartyV1 = InstitutionPartyV1(
    id = institutionPartyId.toString,
    externalId = externalId2.toString,
    originId = originId2.toString,
    description = description,
    digitalAddress = digitalAddress,
    address = address,
    zipCode = zipCode,
    taxCode = taxCode,
    start = start.asFormattedString.success.value,
    end = None,
    origin = origin,
    institutionType = Option(institutionType),
    products = products.toSeq.map(p =>
      InstitutionProductV1(
        product = p.product,
        billing = BillingV1(p.billing.vatNumber, p.billing.recipientCode, p.billing.publicServices),
        pricingPlan = p.pricingPlan
      )
    ),
    attributes = attributes.map(attr =>
      InstitutionAttributeV1(origin = attr.origin, code = attr.code, description = attr.description)
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
    )
  )

  val relationshipV1: PartyRelationshipV1 =
    createPartyRelationshipV1(
      relationshipId,
      PartyRelationshipV1.PartyRoleV1.MANAGER,
      PartyRelationshipV1.PartyRelationshipStateV1.ACTIVE
    )

  val legalV1: PartyRelationshipBindingV1 =
    PartyRelationshipBindingV1(institutionPartyId.toString, managerPendingId.toString)
  val legalV2: PartyRelationshipBindingV1 =
    PartyRelationshipBindingV1(institutionPartyId.toString, delegatePendingId.toString)

  val contractInfoV1: OnboardingContractInfoV1 = OnboardingContractInfoV1(version = version, path = path)

  val tokenV1: TokenV1 =
    TokenV1(
      id = tokenId.toString,
      legals = Seq(legalV1, legalV2),
      validity = validity.asFormattedString.success.value,
      checksum = checksum,
      contractInfo = contractInfoV1
    )

  final val stateV1: StateV1 = {

    val partiesV1: Seq[PartiesV1] =
      Seq(
        PartiesV1(key = personPartyId.toString, value = personPartyV1),
        PartiesV1(key = institutionPartyId.toString, value = institutionPartyV1)
      )

    val noneTestV1 =
      createPartyRelationshipV1(
        noneTestId,
        PartyRelationshipV1.PartyRoleV1.MANAGER,
        PartyRelationshipV1.PartyRelationshipStateV1.ACTIVE
      ).copy(updatedAt = None, filePath = None, fileName = None, contentType = None, onboardingTokenId = None)

    val managerActiveV1 = createPartyRelationshipV1(
      managerActiveId,
      PartyRelationshipV1.PartyRoleV1.MANAGER,
      PartyRelationshipV1.PartyRelationshipStateV1.ACTIVE
    )

    val managerPendingV1 = createPartyRelationshipV1(
      managerPendingId,
      PartyRelationshipV1.PartyRoleV1.MANAGER,
      PartyRelationshipV1.PartyRelationshipStateV1.PENDING
    )

    val managerRejectedV1 = createPartyRelationshipV1(
      managerRejectedId,
      PartyRelationshipV1.PartyRoleV1.MANAGER,
      PartyRelationshipV1.PartyRelationshipStateV1.REJECTED
    )

    val managerSuspendedV1 = createPartyRelationshipV1(
      managerSuspendedId,
      PartyRelationshipV1.PartyRoleV1.MANAGER,
      PartyRelationshipV1.PartyRelationshipStateV1.SUSPENDED
    )

    val managerDeletedV1 = createPartyRelationshipV1(
      managerDeletedId,
      PartyRelationshipV1.PartyRoleV1.MANAGER,
      PartyRelationshipV1.PartyRelationshipStateV1.DELETED
    )

    val delegateActiveV1 = createPartyRelationshipV1(
      delegateActiveId,
      PartyRelationshipV1.PartyRoleV1.DELEGATE,
      PartyRelationshipV1.PartyRelationshipStateV1.ACTIVE
    )

    val delegatePendingV1 = createPartyRelationshipV1(
      delegatePendingId,
      PartyRelationshipV1.PartyRoleV1.DELEGATE,
      PartyRelationshipV1.PartyRelationshipStateV1.PENDING
    )

    val delegateRejectedV1 = createPartyRelationshipV1(
      delegateRejectedId,
      PartyRelationshipV1.PartyRoleV1.DELEGATE,
      PartyRelationshipV1.PartyRelationshipStateV1.REJECTED
    )

    val delegateSuspendedV1 = createPartyRelationshipV1(
      delegateSuspendedId,
      PartyRelationshipV1.PartyRoleV1.DELEGATE,
      PartyRelationshipV1.PartyRelationshipStateV1.SUSPENDED
    )

    val delegateDeletedV1 = createPartyRelationshipV1(
      delegateDeletedId,
      PartyRelationshipV1.PartyRoleV1.DELEGATE,
      PartyRelationshipV1.PartyRelationshipStateV1.DELETED
    )

    val subDelegateActiveV1 = createPartyRelationshipV1(
      subDelegateActiveId,
      PartyRelationshipV1.PartyRoleV1.SUB_DELEGATE,
      PartyRelationshipV1.PartyRelationshipStateV1.ACTIVE
    )

    val subDelegatePendingV1 = createPartyRelationshipV1(
      subDelegatePendingId,
      PartyRelationshipV1.PartyRoleV1.SUB_DELEGATE,
      PartyRelationshipV1.PartyRelationshipStateV1.PENDING
    )

    val subDelegateRejectedV1 = createPartyRelationshipV1(
      subDelegateRejectedId,
      PartyRelationshipV1.PartyRoleV1.SUB_DELEGATE,
      PartyRelationshipV1.PartyRelationshipStateV1.REJECTED
    )

    val subDelegateSuspendedV1 = createPartyRelationshipV1(
      subDelegateSuspendedId,
      PartyRelationshipV1.PartyRoleV1.SUB_DELEGATE,
      PartyRelationshipV1.PartyRelationshipStateV1.SUSPENDED
    )

    val subDelegateDeletedV1 = createPartyRelationshipV1(
      subDelegateDeletedId,
      PartyRelationshipV1.PartyRoleV1.SUB_DELEGATE,
      PartyRelationshipV1.PartyRelationshipStateV1.DELETED
    )

    val operatorActiveV1 = createPartyRelationshipV1(
      operatorActiveId,
      PartyRelationshipV1.PartyRoleV1.OPERATOR,
      PartyRelationshipV1.PartyRelationshipStateV1.ACTIVE
    )

    val operatorPendingV1 = createPartyRelationshipV1(
      operatorPendingId,
      PartyRelationshipV1.PartyRoleV1.OPERATOR,
      PartyRelationshipV1.PartyRelationshipStateV1.PENDING
    )

    val operatorRejectedV1 = createPartyRelationshipV1(
      operatorRejectedId,
      PartyRelationshipV1.PartyRoleV1.OPERATOR,
      PartyRelationshipV1.PartyRelationshipStateV1.REJECTED
    )

    val operatorSuspendedV1 = createPartyRelationshipV1(
      operatorSuspendedId,
      PartyRelationshipV1.PartyRoleV1.OPERATOR,
      PartyRelationshipV1.PartyRelationshipStateV1.SUSPENDED
    )

    val operatorDeletedV1 = createPartyRelationshipV1(
      operatorDeletedId,
      PartyRelationshipV1.PartyRoleV1.OPERATOR,
      PartyRelationshipV1.PartyRelationshipStateV1.DELETED
    )

    val tokensV1: Seq[TokensV1] =
      Seq(TokensV1(key = tokenId.toString, value = tokenV1))

    val relationshipsV1: Seq[RelationshipEntryV1] = Seq(
      RelationshipEntryV1(key = noneTestV1.id, value = noneTestV1),
      RelationshipEntryV1(key = managerActiveV1.id, value = managerActiveV1),
      RelationshipEntryV1(key = managerPendingV1.id, value = managerPendingV1),
      RelationshipEntryV1(key = managerRejectedV1.id, value = managerRejectedV1),
      RelationshipEntryV1(key = managerDeletedV1.id, value = managerDeletedV1),
      RelationshipEntryV1(key = managerSuspendedV1.id, value = managerSuspendedV1),
      RelationshipEntryV1(key = delegateActiveV1.id, value = delegateActiveV1),
      RelationshipEntryV1(key = delegatePendingV1.id, value = delegatePendingV1),
      RelationshipEntryV1(key = delegateRejectedV1.id, value = delegateRejectedV1),
      RelationshipEntryV1(key = delegateDeletedV1.id, value = delegateDeletedV1),
      RelationshipEntryV1(key = delegateSuspendedV1.id, value = delegateSuspendedV1),
      RelationshipEntryV1(key = subDelegateActiveV1.id, value = subDelegateActiveV1),
      RelationshipEntryV1(key = subDelegatePendingV1.id, value = subDelegatePendingV1),
      RelationshipEntryV1(key = subDelegateRejectedV1.id, value = subDelegateRejectedV1),
      RelationshipEntryV1(key = subDelegateDeletedV1.id, value = subDelegateDeletedV1),
      RelationshipEntryV1(key = subDelegateSuspendedV1.id, value = subDelegateSuspendedV1),
      RelationshipEntryV1(key = operatorActiveV1.id, value = operatorActiveV1),
      RelationshipEntryV1(key = operatorPendingV1.id, value = operatorPendingV1),
      RelationshipEntryV1(key = operatorRejectedV1.id, value = operatorRejectedV1),
      RelationshipEntryV1(key = operatorDeletedV1.id, value = operatorDeletedV1),
      RelationshipEntryV1(key = operatorSuspendedV1.id, value = operatorSuspendedV1)
    )

    StateV1(parties = partiesV1, tokens = tokensV1, relationships = relationshipsV1)
  }
}
