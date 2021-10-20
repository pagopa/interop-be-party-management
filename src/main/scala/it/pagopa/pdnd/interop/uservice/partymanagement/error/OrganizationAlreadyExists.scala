package it.pagopa.pdnd.interop.uservice.partymanagement.error

final case class OrganizationAlreadyExists(externalId: String)
    extends Throwable(s"Organization with external ID $externalId already exists")
