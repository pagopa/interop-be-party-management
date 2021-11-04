package it.pagopa.pdnd.interop.uservice.partymanagement.model

import java.util.UUID

/** @param id  for example: ''null''
  * @param from person ID for example: ''null''
  * @param to organization ID for example: ''null''
  * @param filePath path of the file containing the signed onboarding document for example: ''null''
  * @param fileName name of the file containing the signed onboarding document for example: ''null''
  * @param contentType content type of the file containing the signed onboarding document for example: ''null''
  * @param role represents the generic available role types for the relationship for example: ''null''
  * @param product if present, it represents the current PagoPA product this relationship belongs to for example: ''null''
  * @param productRole user role in the application context (e.g.: administrator, security user). This MUST belong to the configured set of application specific product roles for example: ''null''
  * @param status  for example: ''null''
  */
final case class Relationship(
  id: UUID,
  from: UUID,
  to: UUID,
  filePath: Option[String],
  fileName: Option[String],
  contentType: Option[String],
  role: String,
  product: Option[String],
  productRole: String,
  status: String
)
