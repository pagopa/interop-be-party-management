package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.{offsetDateTimeFormat, uuidFormat}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.TokenSeed
import spray.json._

import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.{Base64, UUID}
import scala.util.Try
import cats.implicits._

final case class Token(legals: Seq[PartyRelationShipId], validity: OffsetDateTime, status: TokenStatus, seed: UUID) {
  def isValid: Boolean = OffsetDateTime.now().isBefore(validity) && status == Waiting

}

@SuppressWarnings(Array("org.wartremover.warts.Any"))
object Token extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val format: RootJsonFormat[Token] = jsonFormat4(Token.apply)

  final val validityHours: Long = 24L

  def generate(tokenSeed: TokenSeed): Either[Throwable, Token] = {
    val parties: Either[Throwable, Seq[PartyRelationShipId]] = tokenSeed.tokenUsers.traverse(tokenUser =>
      PartyRole
        .fromText(tokenUser.role.value)
        .map(role => PartyRelationShipId(UUID.fromString(tokenUser.from), UUID.fromString(tokenUser.to), role))
    )

    parties.map(pts =>
      Token(
        seed = UUID.fromString(tokenSeed.seed),
        legals = pts,
        validity = OffsetDateTime.now().plusHours(validityHours),
        status = Waiting
      )
    )

  }

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
