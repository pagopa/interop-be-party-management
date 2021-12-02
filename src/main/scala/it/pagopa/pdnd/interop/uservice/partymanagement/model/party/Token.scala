package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.pdnd.interop.commons.utils.SprayCommonFormats.{offsetDateTimeFormat, uuidFormat}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.TokenSeed
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

final case class Token(
  id: UUID,
  checksum: String,
  applicationId: String,
  legals: Seq[PartyRelationshipBinding],
  validity: OffsetDateTime
) {
  def isValid: Boolean = OffsetDateTime.now().isBefore(validity)

}

object Token extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val partyRelationshipFormat: RootJsonFormat[PartyRelationshipBinding] = jsonFormat2(
    PartyRelationshipBinding.apply
  )
  implicit val format: RootJsonFormat[Token] = jsonFormat5(Token.apply)

  final val validityHours: Long = 24L

  def generate(
    tokenSeed: TokenSeed,
    parties: Seq[PersistedPartyRelationship],
    timestamp: OffsetDateTime
  ): Either[Throwable, Token] =
    parties
      .find(_.role == Manager)
      .map(managerRelationship =>
        Token(
          id = UUID.fromString(tokenSeed.id),
          applicationId = managerRelationship.applicationId,
          legals = parties.map(r => PartyRelationshipBinding(r.from, r.id)),
          checksum = tokenSeed.checksum,
          validity = timestamp.plusHours(validityHours)
        )
      )
      .toRight(new RuntimeException("Token can't be generated because non manager party has been supplied"))

}
