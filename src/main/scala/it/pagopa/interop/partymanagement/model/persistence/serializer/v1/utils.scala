package it.pagopa.interop.partymanagement.model.persistence.serializer.v1

import cats.implicits._
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.partymanagement.common.utils.ErrorOr
import it.pagopa.interop.partymanagement.model.party.PersistedPartyRelationshipState._
import it.pagopa.interop.partymanagement.model.party._
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1.party.PartyV1.Empty
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1.party.{
  DataProtectionOfficerV1,
  InstitutionAttributeV1,
  InstitutionPartyV1,
  InstitutionProductV1,
  PartyV1,
  PaymentServiceProviderV1,
  PersonPartyV1
}
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1.relationship.PartyRelationshipV1.{
  PartyRelationshipStateV1,
  PartyRoleV1
}
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1.relationship.{
  BillingV1,
  InstitutionUpdateV1,
  PartyRelationshipProductV1,
  PartyRelationshipV1
}
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1.state.{
  PartiesV1,
  RelationshipEntryV1,
  TokensV1
}
import it.pagopa.interop.partymanagement.model.persistence.serializer.v1.token.{
  OnboardingContractInfoV1,
  PartyRelationshipBindingV1,
  TokenV1
}

import java.util.UUID
import scala.util.Try

object utils {

  def getParty(partyV1: PartyV1): ErrorOr[Party] = partyV1 match {
    case p: PersonPartyV1 =>
      {
        for {
          start <- p.start.toOffsetDateTime
          end   <- p.end.traverse(_.toOffsetDateTime)
        } yield PersonParty(id = UUID.fromString(p.id), start = start, end = end)
      }.toEither

    case i: InstitutionPartyV1 =>
      {
        for {
          start <- i.start.toOffsetDateTime
          end   <- i.end.traverse(_.toOffsetDateTime)
        } yield InstitutionParty(
          id = UUID.fromString(i.id),
          externalId = i.externalId,
          originId = i.originId,
          description = i.description,
          digitalAddress = i.digitalAddress,
          taxCode = i.taxCode,
          address = i.address,
          zipCode = i.zipCode,
          origin = i.origin,
          institutionType = i.institutionType,
          products = i.products
            .map(p => PersistedInstitutionProduct(p.product, p.pricingPlan, getPersistedBilling(p.billing)))
            .toSet,
          attributes = i.attributes
            .map(a => InstitutionAttribute(origin = a.origin, code = a.code, description = a.description))
            .toSet,
          start = start,
          end = end,
          paymentServiceProvider = i.paymentServiceProvider
            .map(p =>
              PersistedPaymentServiceProvider(
                abiCode = p.abiCode,
                businessRegisterNumber = p.businessRegisterNumber,
                legalRegisterName = p.legalRegisterName,
                legalRegisterNumber = p.legalRegisterNumber,
                vatNumberGroup = p.vatNumberGroup
              )
            ),
          dataProtectionOfficer = i.dataProtectionOfficer
            .map(d => PersistedDataProtectionOfficer(address = d.address, email = d.email, pec = d.pec))
        )
      }.toEither
    case Empty                 => Left(new RuntimeException("Deserialization from protobuf failed"))
  }

  def getPartyV1(party: Party): ErrorOr[PartyV1] = party match {
    case p: PersonParty      =>
      {
        for {
          start <- p.start.asFormattedString
          end   <- p.end.traverse(_.asFormattedString)
        } yield PersonPartyV1(id = p.id.toString, start = start, end = end)
      }.toEither
    case i: InstitutionParty =>
      {
        for {
          start <- i.start.asFormattedString
          end   <- i.end.traverse(_.asFormattedString)
        } yield InstitutionPartyV1(
          id = i.id.toString,
          externalId = i.externalId,
          originId = i.originId,
          description = i.description,
          digitalAddress = i.digitalAddress,
          taxCode = i.taxCode,
          address = i.address,
          zipCode = i.zipCode,
          origin = i.origin,
          institutionType = i.institutionType,
          products = i.products
            .map(p =>
              InstitutionProductV1(product = p.product, billing = getBillingV1(p.billing), pricingPlan = p.pricingPlan)
            )
            .toSeq,
          attributes = i.attributes
            .map(a => InstitutionAttributeV1(origin = a.origin, code = a.code, description = a.description))
            .toSeq,
          paymentServiceProvider = i.paymentServiceProvider
            .map(p =>
              PaymentServiceProviderV1(
                abiCode = p.abiCode,
                businessRegisterNumber = p.businessRegisterNumber,
                legalRegisterName = p.legalRegisterName,
                legalRegisterNumber = p.legalRegisterNumber,
                vatNumberGroup = p.vatNumberGroup
              )
            ),
          dataProtectionOfficer = i.dataProtectionOfficer
            .map(a => DataProtectionOfficerV1(address = a.address, email = a.email, pec = a.pec)),
          start = start,
          end = end
        )
      }.toEither
  }

  def stringToUUID(uuidStr: String): ErrorOr[UUID] =
    Try { UUID.fromString(uuidStr) }.toEither

  def getPartyRelationship(partyRelationshipV1: PartyRelationshipV1): ErrorOr[PersistedPartyRelationship] = {
    for {
      id                <- stringToUUID(partyRelationshipV1.id)
      from              <- stringToUUID(partyRelationshipV1.from)
      to                <- stringToUUID(partyRelationshipV1.to)
      partyRole         <- partyRoleFromProtobuf(partyRelationshipV1.role)
      state             <- relationshipStateFromProtobuf(partyRelationshipV1.state)
      onboardingTokenId <- partyRelationshipV1.onboardingTokenId.traverse(_.toUUID.toEither)
      createdAt         <- partyRelationshipV1.createdAt.toOffsetDateTime.toEither
      updatedAt         <- partyRelationshipV1.updatedAt.traverse(_.toOffsetDateTime).toEither
      timestamp         <- partyRelationshipV1.product.createdAt.toOffsetDateTime.toEither
    } yield PersistedPartyRelationship(
      id = id,
      from = from,
      to = to,
      role = partyRole,
      product = PersistedProduct(
        id = partyRelationshipV1.product.id,
        role = partyRelationshipV1.product.role,
        createdAt = timestamp
      ),
      createdAt = createdAt,
      updatedAt = updatedAt,
      state = state,
      filePath = partyRelationshipV1.filePath,
      fileName = partyRelationshipV1.fileName,
      contentType = partyRelationshipV1.contentType,
      onboardingTokenId = onboardingTokenId,
      pricingPlan = partyRelationshipV1.pricingPlan,
      institutionUpdate = partyRelationshipV1.institutionUpdate.map(i =>
        PersistedInstitutionUpdate(
          institutionType = i.institutionType,
          description = i.description,
          digitalAddress = i.digitalAddress,
          zipCode = i.zipCode,
          address = i.address,
          taxCode = i.taxCode
        )
      ),
      billing = partyRelationshipV1.billing.map(getPersistedBilling)
    )
  }

  private def getPersistedBilling(b: BillingV1): PersistedBilling =
    PersistedBilling(vatNumber = b.vatNumber, recipientCode = b.recipientCode, publicServices = b.publicServices)

  def getPartyRelationshipV1(partyRelationship: PersistedPartyRelationship): PartyRelationshipV1 =
    PartyRelationshipV1(
      id = partyRelationship.id.toString,
      from = partyRelationship.from.toString,
      to = partyRelationship.to.toString,
      role = partyRoleToProtobuf(partyRelationship.role),
      product = PartyRelationshipProductV1(
        id = partyRelationship.product.id,
        role = partyRelationship.product.role,
        createdAt = partyRelationship.product.createdAt.toMillis
      ),
      createdAt = partyRelationship.createdAt.toMillis,
      updatedAt = partyRelationship.updatedAt.map(_.toMillis),
      state = relationshipStateToProtobuf(partyRelationship.state),
      filePath = partyRelationship.filePath,
      fileName = partyRelationship.fileName,
      contentType = partyRelationship.contentType,
      onboardingTokenId = partyRelationship.onboardingTokenId.map(_.toString),
      pricingPlan = partyRelationship.pricingPlan,
      institutionUpdate = partyRelationship.institutionUpdate.map(i =>
        InstitutionUpdateV1(
          institutionType = i.institutionType,
          description = i.description,
          digitalAddress = i.digitalAddress,
          address = i.address,
          zipCode = i.zipCode,
          taxCode = i.taxCode
        )
      ),
      billing = partyRelationship.billing.map(getBillingV1)
    )

  private def getBillingV1(b: PersistedBilling): BillingV1 =
    BillingV1(vatNumber = b.vatNumber, recipientCode = b.recipientCode, publicServices = b.publicServices)

  def getToken(tokenV1: TokenV1): ErrorOr[Token] = {
    for {
      id       <- tokenV1.id.toUUID.toEither
      legals   <- tokenV1.legals.traverse(partyRelationshipBindingMapper)
      validity <- tokenV1.validity.toOffsetDateTime.toEither
    } yield Token(
      id = id,
      legals = legals,
      validity = validity,
      checksum = tokenV1.checksum,
      contractInfo =
        TokenOnboardingContractInfo(version = tokenV1.contractInfo.version, path = tokenV1.contractInfo.path)
    )
  }

  def partyRelationshipBindingMapper(
    partyRelationshipBindingV1: PartyRelationshipBindingV1
  ): ErrorOr[PartyRelationshipBinding] = {
    for {
      partyId        <- stringToUUID(partyRelationshipBindingV1.partyId)
      relationshipId <- stringToUUID(partyRelationshipBindingV1.relationshipId)
    } yield PartyRelationshipBinding(partyId = partyId, relationshipId = relationshipId)
  }

  def getTokenV1(token: Token): ErrorOr[TokenV1] = {
    token.validity.asFormattedString.toEither.map(validity =>
      TokenV1(
        id = token.id.toString,
        legals =
          token.legals.map(legal => PartyRelationshipBindingV1(legal.partyId.toString, legal.relationshipId.toString)),
        validity = validity,
        checksum = token.checksum,
        contractInfo = OnboardingContractInfoV1(version = token.contractInfo.version, path = token.contractInfo.path)
      )
    )

  }

  def extractTupleFromPartiesV1(partiesV1: PartiesV1): Either[Throwable, (UUID, Party)] = {
    for {
      key   <- partiesV1.key.toUUID.toEither
      party <- getParty(partiesV1.value)
    } yield key -> party
  }

  def extractTupleFromTokensV1(tokensV1: TokensV1): Either[Throwable, (UUID, Token)] = {
    for {
      key   <- tokensV1.key.toUUID.toEither
      token <- getToken(tokensV1.value)
    } yield key -> token
  }

  def extractTupleFromRelationshipEntryV1(
    relationshipEntryV1: RelationshipEntryV1
  ): Either[Throwable, (UUID, PersistedPartyRelationship)] = {
    for {
      key   <- relationshipEntryV1.key.toUUID.toEither
      value <- getPartyRelationship(relationshipEntryV1.value)
    } yield key -> value
  }

  def partyRoleFromProtobuf(role: PartyRoleV1): ErrorOr[PersistedPartyRole] =
    role match {
      case PartyRoleV1.MANAGER             => Right(PersistedPartyRole.Manager)
      case PartyRoleV1.DELEGATE            => Right(PersistedPartyRole.Delegate)
      case PartyRoleV1.SUB_DELEGATE        => Right(PersistedPartyRole.SubDelegate)
      case PartyRoleV1.OPERATOR            => Right(PersistedPartyRole.Operator)
      case PartyRoleV1.Unrecognized(value) =>
        Left(new RuntimeException(s"Unable to deserialize party role value $value"))
    }

  def relationshipStateFromProtobuf(state: PartyRelationshipStateV1): ErrorOr[PersistedPartyRelationshipState] =
    state match {
      case PartyRelationshipStateV1.PENDING             => Right(Pending)
      case PartyRelationshipStateV1.ACTIVE              => Right(Active)
      case PartyRelationshipStateV1.SUSPENDED           => Right(Suspended)
      case PartyRelationshipStateV1.DELETED             => Right(Deleted)
      case PartyRelationshipStateV1.REJECTED            => Right(Rejected)
      case PartyRelationshipStateV1.TOBEVALIDATED       => Right(ToBeValidated)
      case PartyRelationshipStateV1.Unrecognized(value) =>
        Left(new RuntimeException(s"Unable to deserialize party relationship state value $value"))
    }

  def partyRoleToProtobuf(role: PersistedPartyRole): PartyRoleV1 =
    role match {
      case PersistedPartyRole.Manager     => PartyRoleV1.MANAGER
      case PersistedPartyRole.Delegate    => PartyRoleV1.DELEGATE
      case PersistedPartyRole.SubDelegate => PartyRoleV1.SUB_DELEGATE
      case PersistedPartyRole.Operator    => PartyRoleV1.OPERATOR
    }

  def relationshipStateToProtobuf(state: PersistedPartyRelationshipState): PartyRelationshipStateV1 =
    state match {
      case Pending       => PartyRelationshipStateV1.PENDING
      case Active        => PartyRelationshipStateV1.ACTIVE
      case Suspended     => PartyRelationshipStateV1.SUSPENDED
      case Deleted       => PartyRelationshipStateV1.DELETED
      case Rejected      => PartyRelationshipStateV1.REJECTED
      case ToBeValidated => PartyRelationshipStateV1.TOBEVALIDATED
    }

}
