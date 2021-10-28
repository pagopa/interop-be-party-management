package it.pagopa.pdnd.interop.uservice.partymanagement.model

/** @param seed  for example: ''97c0f418-bcb3-48d4-825a-fe8b29ae68e5''
  * @param relationships  for example: ''null''
  * @param checksum  for example: ''null''
  */
final case class TokenSeed(seed: String, relationships: RelationshipsSeed, checksum: String)
