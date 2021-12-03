package it.pagopa.pdnd.interop.uservice.partymanagement.error

final case class ManagerNotSupplied(tokenId: String)
    extends Throwable(s"Token $tokenId can't be generated because no manager party has been supplied")
