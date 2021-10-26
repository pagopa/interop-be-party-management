package it.pagopa.pdnd.interop.uservice.partymanagement.model

import java.util.UUID

/** @param id  for example: ''97c0f418-bcb3-48d4-825a-fe8b29ae68e5''
  * @param institutionId DN for example: ''aoo=c_f205,o=c_f205,c=it''
  * @param description  for example: ''AGENCY X''
  * @param digitalAddress  for example: ''email@pec.mail.org''
  * @param attributes  for example: ''null''
  */
final case class Organization(
  id: UUID,
  institutionId: String,
  description: String,
  digitalAddress: String,
  attributes: Seq[String]
)
