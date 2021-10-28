package it.pagopa.pdnd.interop.uservice.partymanagement.model

import java.util.UUID

/** @param from person ID for example: ''null''
  * @param to organization ID for example: ''null''
  * @param role represents the generic available role types for the relationship for example: ''null''
  * @param platformRole user role in the application context (e.g.: administrator, security user). This MUST belong to the configured set of application specific platform roles for example: ''null''
  */
final case class RelationshipSeed(from: UUID, to: UUID, role: String, platformRole: String)
