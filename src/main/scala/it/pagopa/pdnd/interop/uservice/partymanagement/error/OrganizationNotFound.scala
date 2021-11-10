package it.pagopa.pdnd.interop.uservice.partymanagement.error

final case class OrganizationNotFound(externalId: String)
    extends Throwable(s"Organization with external ID $externalId not found")
