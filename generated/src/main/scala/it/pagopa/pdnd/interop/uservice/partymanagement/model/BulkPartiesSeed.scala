package it.pagopa.pdnd.interop.uservice.partymanagement.model

import java.util.UUID

/** @param partyIdentifiers the identifiers of party for example: ''null''
  */
final case class BulkPartiesSeed(partyIdentifiers: Seq[UUID])
