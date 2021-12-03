package it.pagopa.pdnd.interop.uservice.partymanagement.error

final case class TokenNotFound(tokenId: String) extends Throwable(s"Token $tokenId not found")
