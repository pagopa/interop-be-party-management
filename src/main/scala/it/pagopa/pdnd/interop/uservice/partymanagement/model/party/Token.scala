package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.{offsetDateTimeFormat, uuidFormat}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.TokenSeed
import spray.json._

import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.{Base64, UUID}
import scala.util.Try

final case class Token(
  id: String,
  legals: Seq[PartyRelationShipId],
  validity: OffsetDateTime,
  status: TokenStatus,
  checksum: String,
  seed: UUID
) {
  def isValid: Boolean = OffsetDateTime.now().isBefore(validity) && status == Waiting

}

@SuppressWarnings(Array("org.wartremover.warts.Any"))
object Token extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val format: RootJsonFormat[Token] = jsonFormat6(Token.apply)

  final val validityHours: Long = 24L

  def generate(tokenSeed: TokenSeed, parties: Seq[PartyRelationShipId]): Either[Throwable, Token] =
    parties
      .find(_.role == Manager)
      .map(manager =>
        Token(
          id = manager.stringify,
          seed = UUID.fromString(tokenSeed.seed),
          legals = parties,
          checksum = tokenSeed.checksum,
          validity = OffsetDateTime.now().plusHours(validityHours),
          status = Waiting
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
