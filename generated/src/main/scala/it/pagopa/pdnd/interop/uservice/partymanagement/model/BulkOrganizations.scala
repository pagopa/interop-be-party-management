package it.pagopa.pdnd.interop.uservice.partymanagement.model

/** @param found the collection of organizations found. for example: ''null''
  * @param notFound the identifiers of organizations not found. for example: ''null''
  */
final case class BulkOrganizations(found: Seq[Organization], notFound: Seq[String])
