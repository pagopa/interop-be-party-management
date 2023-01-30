package it.pagopa.interop.partymanagement.persistence.v1

import it.pagopa.interop.commons.utils.TypeConversions.{LongOps, OffsetDateTimeOps}
import it.pagopa.interop.partymanagement.model.party._
import it.pagopa.interop.partymanagement.model.persistence.State
import org.scalatest.TryValues._

import java.util.UUID

object StateData {

  import StateCommonData._

  val product: PersistedProduct = PersistedProduct(
    id = productId,
    role = productRole,
    createdAt = productCreatedAt.toMillis.toOffsetDateTime.success.value
  )

  def createPartyRelationship(
    partyRelationshipId: UUID,
    role: PersistedPartyRole,
    state: PersistedPartyRelationshipState
  ): PersistedPartyRelationship = PersistedPartyRelationship(
    id = partyRelationshipId,
    from = personPartyId,
    to = institutionPartyId,
    role = role,
    product = product,
    createdAt = createdAt.toMillis.toOffsetDateTime.success.value,
    updatedAt = Some(updatedAt.toMillis.toOffsetDateTime.success.value),
    state = state,
    filePath = Some(filePath),
    fileName = Some(fileName),
    contentType = Some(contentType),
    onboardingTokenId = Some(onboardingTokenId),
    pricingPlan = pricingPlan,
    institutionUpdate = institutionUpdate,
    billing = billing
  )

  val personParty: PersonParty = PersonParty(id = personPartyId, start = start, end = Some(end))

  val institutionParty: InstitutionParty = InstitutionParty(
    id = institutionPartyId,
    externalId = externalId2.toString,
    originId = originId2.toString,
    description = description,
    digitalAddress = digitalAddress,
    address = address,
    zipCode = zipCode,
    taxCode = taxCode,
    start = start,
    end = None,
    origin = origin,
    institutionType = Option(institutionType),
    products = products,
    attributes = attributes.toSet,
    paymentServiceProvider = Option(
      PersistedPaymentServiceProvider(
        abiCode = paymentServiceProvider.abiCode,
        businessRegisterNumber = paymentServiceProvider.businessRegisterNumber,
        legalRegisterName = paymentServiceProvider.legalRegisterName,
        legalRegisterNumber = paymentServiceProvider.legalRegisterNumber,
        vatNumberGroup = paymentServiceProvider.vatNumberGroup
      )
    ),
    dataProtectionOfficer = Option(
      PersistedDataProtectionOfficer(
        address = dataProtectionOfficer.address,
        email = dataProtectionOfficer.email,
        pec = dataProtectionOfficer.pec
      )
    ),
    geographicTaxonomies = geographicTaxonomies,
    rea = None,
    shareCapital = None,
    businessRegisterPlace = None,
    supportEmail = None,
    supportPhone = None,
    imported = None
  )

  val relationship: PersistedPartyRelationship =
    createPartyRelationship(relationshipId, PersistedPartyRole.Manager, PersistedPartyRelationshipState.Active)

  val legal1: PartyRelationshipBinding = PartyRelationshipBinding(institutionPartyId, managerPendingId)
  val legal2: PartyRelationshipBinding = PartyRelationshipBinding(institutionPartyId, delegatePendingId)

  val contractInfo: TokenOnboardingContractInfo = TokenOnboardingContractInfo(version = version, path = path)

  val token: Token =
    Token(
      id = tokenId,
      legals = Seq(legal1, legal2),
      validity = validity,
      checksum = checksum,
      contractInfo = contractInfo
    )

  final val state: State = {

    val parties: Map[UUID, Party] =
      Map(personParty.id -> personParty, institutionParty.id -> institutionParty)

    val noneTest =
      createPartyRelationship(noneTestId, PersistedPartyRole.Manager, PersistedPartyRelationshipState.Active).copy(
        updatedAt = None,
        filePath = None,
        fileName = None,
        contentType = None,
        onboardingTokenId = None
      )

    val managerActive =
      createPartyRelationship(managerActiveId, PersistedPartyRole.Manager, PersistedPartyRelationshipState.Active)

    val managerPending =
      createPartyRelationship(managerPendingId, PersistedPartyRole.Manager, PersistedPartyRelationshipState.Pending)

    val managerRejected =
      createPartyRelationship(managerRejectedId, PersistedPartyRole.Manager, PersistedPartyRelationshipState.Rejected)

    val managerSuspended =
      createPartyRelationship(managerSuspendedId, PersistedPartyRole.Manager, PersistedPartyRelationshipState.Suspended)

    val managerDeleted =
      createPartyRelationship(managerDeletedId, PersistedPartyRole.Manager, PersistedPartyRelationshipState.Deleted)

    val delegateActive =
      createPartyRelationship(delegateActiveId, PersistedPartyRole.Delegate, PersistedPartyRelationshipState.Active)

    val delegatePending =
      createPartyRelationship(delegatePendingId, PersistedPartyRole.Delegate, PersistedPartyRelationshipState.Pending)

    val delegateRejected =
      createPartyRelationship(delegateRejectedId, PersistedPartyRole.Delegate, PersistedPartyRelationshipState.Rejected)

    val delegateSuspended = createPartyRelationship(
      delegateSuspendedId,
      PersistedPartyRole.Delegate,
      PersistedPartyRelationshipState.Suspended
    )

    val delegateDeleted =
      createPartyRelationship(delegateDeletedId, PersistedPartyRole.Delegate, PersistedPartyRelationshipState.Deleted)

    val subDelegateActive = createPartyRelationship(
      subDelegateActiveId,
      PersistedPartyRole.SubDelegate,
      PersistedPartyRelationshipState.Active
    )

    val subDelegatePending = createPartyRelationship(
      subDelegatePendingId,
      PersistedPartyRole.SubDelegate,
      PersistedPartyRelationshipState.Pending
    )

    val subDelegateRejected = createPartyRelationship(
      subDelegateRejectedId,
      PersistedPartyRole.SubDelegate,
      PersistedPartyRelationshipState.Rejected
    )

    val subDelegateSuspended = createPartyRelationship(
      subDelegateSuspendedId,
      PersistedPartyRole.SubDelegate,
      PersistedPartyRelationshipState.Suspended
    )

    val subDelegateDeleted = createPartyRelationship(
      subDelegateDeletedId,
      PersistedPartyRole.SubDelegate,
      PersistedPartyRelationshipState.Deleted
    )

    val operatorActive =
      createPartyRelationship(operatorActiveId, PersistedPartyRole.Operator, PersistedPartyRelationshipState.Active)

    val operatorPending =
      createPartyRelationship(operatorPendingId, PersistedPartyRole.Operator, PersistedPartyRelationshipState.Pending)

    val operatorRejected =
      createPartyRelationship(operatorRejectedId, PersistedPartyRole.Operator, PersistedPartyRelationshipState.Rejected)

    val operatorSuspended = createPartyRelationship(
      operatorSuspendedId,
      PersistedPartyRole.Operator,
      PersistedPartyRelationshipState.Suspended
    )

    val operatorDeleted =
      createPartyRelationship(operatorDeletedId, PersistedPartyRole.Operator, PersistedPartyRelationshipState.Deleted)

    val tokens: Map[UUID, Token] = Map(token.id -> token)

    val relationships: Map[UUID, PersistedPartyRelationship] = Map(
      noneTest.id             -> noneTest,
      managerActive.id        -> managerActive,
      managerPending.id       -> managerPending,
      managerRejected.id      -> managerRejected,
      managerDeleted.id       -> managerDeleted,
      managerSuspended.id     -> managerSuspended,
      delegateActive.id       -> delegateActive,
      delegatePending.id      -> delegatePending,
      delegateRejected.id     -> delegateRejected,
      delegateDeleted.id      -> delegateDeleted,
      delegateSuspended.id    -> delegateSuspended,
      subDelegateActive.id    -> subDelegateActive,
      subDelegatePending.id   -> subDelegatePending,
      subDelegateRejected.id  -> subDelegateRejected,
      subDelegateDeleted.id   -> subDelegateDeleted,
      subDelegateSuspended.id -> subDelegateSuspended,
      operatorActive.id       -> operatorActive,
      operatorPending.id      -> operatorPending,
      operatorRejected.id     -> operatorRejected,
      operatorDeleted.id      -> operatorDeleted,
      operatorSuspended.id    -> operatorSuspended
    )

    State(parties = parties, tokens = tokens, relationships = relationships)
  }

}
