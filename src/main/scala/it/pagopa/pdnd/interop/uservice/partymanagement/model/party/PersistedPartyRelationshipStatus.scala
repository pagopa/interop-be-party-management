package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.model._

sealed trait PersistedPartyRelationshipStatus {
  def toApi: RelationshipStatus = this match {
    case PersistedPartyRelationshipStatus.Pending   => RelationshipStatus.PENDING
    case PersistedPartyRelationshipStatus.Active    => RelationshipStatus.ACTIVE
    case PersistedPartyRelationshipStatus.Suspended => RelationshipStatus.SUSPENDED
    case PersistedPartyRelationshipStatus.Deleted   => RelationshipStatus.DELETED
    case PersistedPartyRelationshipStatus.Rejected  => RelationshipStatus.REJECTED
  }
}

object PersistedPartyRelationshipStatus {

  case object Pending   extends PersistedPartyRelationshipStatus
  case object Active    extends PersistedPartyRelationshipStatus
  case object Suspended extends PersistedPartyRelationshipStatus
  case object Deleted   extends PersistedPartyRelationshipStatus
  case object Rejected  extends PersistedPartyRelationshipStatus

  def fromApi(status: RelationshipStatus): PersistedPartyRelationshipStatus = status match {
    case RelationshipStatus.PENDING   => Pending
    case RelationshipStatus.ACTIVE    => Active
    case RelationshipStatus.SUSPENDED => Suspended
    case RelationshipStatus.DELETED   => Deleted
    case RelationshipStatus.REJECTED  => Rejected
  }
}
