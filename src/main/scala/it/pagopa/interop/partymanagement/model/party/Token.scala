package it.pagopa.interop.partymanagement.model.party

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.interop.commons.utils.SprayCommonFormats.{offsetDateTimeFormat, uuidFormat}
import it.pagopa.interop.commons.utils.TypeConversions.StringOps
import it.pagopa.interop.partymanagement.common.system.ApplicationConfiguration
import it.pagopa.interop.partymanagement.error.PartyManagementErrors.ManagerNotSupplied
import it.pagopa.interop.partymanagement.model.TokenSeed
import spray.json._

import java.time.OffsetDateTime
import java.util.UUID

/** Models the binding between a party and a relationship.
  * <br>
  * It is used to persist the proper binding in the onboarding token.
  *
  * @param partyId
  * @param relationshipId
  */
//TODO evaluate an Akka persistence alternative to preserve the same behavior without this case class.
final case class PartyRelationshipBinding(partyId: UUID, relationshipId: UUID)

final case class TokenOnboardingContractInfo(version: String, path: String)

final case class Token(
  id: UUID,
  checksum: String,
  legals: Seq[PartyRelationshipBinding],
  validity: OffsetDateTime,
  contractInfo: TokenOnboardingContractInfo
) {
  def isValid: Boolean = OffsetDateTime.now().isBefore(validity)
}

object Token extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val partyRelationshipFormat: RootJsonFormat[PartyRelationshipBinding] = jsonFormat2(
    PartyRelationshipBinding.apply
  )

  implicit val contractInfoFormat: RootJsonFormat[TokenOnboardingContractInfo] = jsonFormat2(
    TokenOnboardingContractInfo.apply
  )
  implicit val format: RootJsonFormat[Token]                                   = jsonFormat5(Token.apply)

  def generate(
    tokenSeed: TokenSeed,
    parties: Seq[PersistedPartyRelationship],
    timestamp: OffsetDateTime
  ): Either[Throwable, Token] = {
    for {
      id <- tokenSeed.id.toUUID.toEither
      _  <- parties
        .find(_.role == PersistedPartyRole.Manager)
        .toRight(ManagerNotSupplied(tokenSeed.id))
    } yield Token(
      id = id,
      legals = parties.map(r => PartyRelationshipBinding(r.from, r.id)),
      checksum = tokenSeed.checksum,
      validity = timestamp.plusHours(ApplicationConfiguration.tokenValidityHours),
      contractInfo = TokenOnboardingContractInfo(tokenSeed.contractInfo.version, tokenSeed.contractInfo.path)
    )

  }

}
