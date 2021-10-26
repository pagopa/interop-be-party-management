package it.pagopa.pdnd.interop.uservice.partymanagement.model

/** @param institutionId DN for example: ''aoo=c_f205,o=c_f205,c=it''
  * @param description  for example: ''AGENCY X''
  * @param digitalAddress  for example: ''email@pec.mail.org''
  * @param attributes  for example: ''null''
  */
final case class OrganizationSeed(
  institutionId: String,
  description: String,
  digitalAddress: String,
  attributes: Seq[String]
)
