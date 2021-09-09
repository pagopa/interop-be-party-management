package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.{offsetDateTimeFormat, uuidFormat}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.TokenSeed
import spray.json._

import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.{Base64, UUID}
import scala.util.Try

/** Models the binding between a party and a relationship.
  * <br>
  * It is used for persist the proper binding within the token.
  *
  * @param partyId
  * @param relationshipId
  */
//TODO evaluate an Akka persistence alternative to preserve the same behavior without this case class.
final case class PartyRelationshipBinding(partyId: UUID, relationshipId: UUID)

@SuppressWarnings(
  Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Equals", "org.wartremover.warts.ToString")
)
final case class Token(
  id: String,
  legals: Seq[PartyRelationshipBinding],
  validity: OffsetDateTime,
  checksum: String,
  seed: UUID
) {
  def isValid: Boolean = OffsetDateTime.now().isBefore(validity)

}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.Equals",
    "org.wartremover.warts.ToString"
  )
)
object Token extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val partyRelationshipFormat: RootJsonFormat[PartyRelationshipBinding] = jsonFormat2(
    PartyRelationshipBinding.apply
  )
  implicit val format: RootJsonFormat[Token] = jsonFormat5(Token.apply)

  final val validityHours: Long = 24L

  def generate(tokenSeed: TokenSeed, parties: Seq[PartyRelationship]): Either[Throwable, Token] =
    parties
      .find(_.role == Manager)
      .map(managerRelationship =>
        Token(
          id = managerRelationship.applicationId,
          seed = UUID.fromString(tokenSeed.seed),
          legals = parties.map(r => PartyRelationshipBinding(r.from, r.id)),
          checksum = tokenSeed.checksum,
          validity = OffsetDateTime.now().plusHours(validityHours)
        )
      )
      .toRight(new RuntimeException("Token can't be generated because non manager party has been supplied"))

  def encode(token: Token): String = {
    val bytes: Array[Byte] = token.toJson.compactPrint.getBytes(StandardCharsets.UTF_8)
    Base64.getEncoder.encodeToString(bytes)
  }

  def decode(code: String): Try[Token] = Try {
    val decoded: Array[Byte] = Base64.getDecoder.decode(code)
    val jsonTxt: String      = new String(decoded, StandardCharsets.UTF_8)
    jsonTxt.parseJson.convertTo[Token]
  }
}
