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
  manager: PartyRelationShipId,
  delegate: PartyRelationShipId,
  validity: OffsetDateTime,
  status: TokenStatus,
  seed: UUID
) {
  def isValid: Boolean = OffsetDateTime.now().isBefore(validity) && status == Waiting

}

object Token extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val format: RootJsonFormat[Token] = jsonFormat5(Token.apply)

  final val validityHours: Long = 24L

  def generate(tokenSeed: TokenSeed): Either[Throwable, Token] = {
    for {
      managerRole  <- PartyRole.fromText(tokenSeed.manager.role.value)
      delegateRole <- PartyRole.fromText(tokenSeed.delegate.role.value)
    } yield Token(
      seed = UUID.fromString(tokenSeed.seed),
      manager = PartyRelationShipId(
        UUID.fromString(tokenSeed.manager.from),
        UUID.fromString(tokenSeed.manager.to),
        managerRole
      ),
      delegate = PartyRelationShipId(
        UUID.fromString(tokenSeed.delegate.from),
        UUID.fromString(tokenSeed.delegate.to),
        delegateRole
      ),
      validity = OffsetDateTime.now().plusHours(validityHours),
      status = Waiting
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
