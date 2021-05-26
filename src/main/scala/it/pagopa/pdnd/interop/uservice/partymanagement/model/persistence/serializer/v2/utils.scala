package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v2

import cats.implicits._
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.{ErrorOr, formatter, toOffsetDateTime}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{Token, TokenStatus}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.token.TokenStatusV1
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.utils.{
  getPartyRelationShipId,
  getPartyRelationShipIdV1
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v2.token.TokenV2

import java.util.UUID

object utils {
  def getToken(tokenV2: TokenV2): ErrorOr[Token] = {
    for {
      legals <- tokenV2.legals.traverse(legal => getPartyRelationShipId(legal))
      status <- TokenStatus.fromText(tokenV2.status.name)
    } yield Token(
      legals = legals,
      validity = toOffsetDateTime(tokenV2.validity),
      status = status,
      seed = UUID.fromString(tokenV2.seed)
    )
  }

  def getTokenV2(token: Token): ErrorOr[TokenV2] = {
    for {
      legals <- token.legals.traverse(legal => getPartyRelationShipIdV1(legal))
      status <- TokenStatusV1
        .fromName(token.status.stringify)
        .toRight(new RuntimeException("Deserialization from protobuf failed"))
    } yield TokenV2(
      legals = legals,
      validity = token.validity.format(formatter),
      status = status,
      seed = token.seed.toString
    )
  }
}
