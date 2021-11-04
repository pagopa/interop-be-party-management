package it.pagopa.pdnd.interop.uservice.partymanagement.model

/** @param institutionId DN for example: ''aoo=c_f205,o=c_f205,c=it''
  * @param code an accessory code (e.g. codice ipa) for example: ''null''
  * @param description  for example: ''AGENCY X''
  * @param digitalAddress  for example: ''email@pec.mail.org''
  * @param fiscalCode organization fiscal code for example: ''null''
  * @param products  for example: ''null''
  * @param attributes  for example: ''null''
  */
final case class OrganizationSeed(
  institutionId: String,
  code: Option[String],
  description: String,
  digitalAddress: String,
  fiscalCode: String,
  products: Seq[String],
  attributes: Seq[String]
)
