package it.pagopa.pdnd.interop.uservice.partymanagement.error

import it.pagopa.pdnd.interop.uservice.partymanagement.model.RelationshipProductSeed
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.PersistedPartyRole

import java.util.UUID

final case class RelationshipAlreadyExists(
  from: UUID,
  to: UUID,
  role: PersistedPartyRole,
  product: RelationshipProductSeed
) extends Throwable(
      s"Relationship from=${from.toString},to=${to.toString},role=${role.toString},product=${product.id}/${product.role} already exists"
    )
