package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.model.{RelationshipProduct, RelationshipProductSeed}

import java.time.OffsetDateTime

final case class PersistedProduct(id: String, role: String, createdAt: OffsetDateTime) {
  def toRelationshipProduct: RelationshipProduct = RelationshipProduct(id = id, role = role, createdAt = createdAt)
}

object PersistedProduct {
  def fromRelationshipProduct(product: RelationshipProductSeed, timestamp: OffsetDateTime): PersistedProduct =
    PersistedProduct(id = product.id, role = product.role, createdAt = timestamp)
}
