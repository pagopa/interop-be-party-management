package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.partymanagement.model._

sealed trait PersistedPartyRelationshipState {
  def toApi: RelationshipState = this match {
    case PersistedPartyRelationshipState.Pending       => RelationshipState.PENDING
    case PersistedPartyRelationshipState.Active        => RelationshipState.ACTIVE
    case PersistedPartyRelationshipState.Suspended     => RelationshipState.SUSPENDED
    case PersistedPartyRelationshipState.Deleted       => RelationshipState.DELETED
    case PersistedPartyRelationshipState.Rejected      => RelationshipState.REJECTED
    case PersistedPartyRelationshipState.ToBeValidated => RelationshipState.TOBEVALIDATED
  }
}

object PersistedPartyRelationshipState {

  case object Pending       extends PersistedPartyRelationshipState
  case object Active        extends PersistedPartyRelationshipState
  case object Suspended     extends PersistedPartyRelationshipState
  case object Deleted       extends PersistedPartyRelationshipState
  case object Rejected      extends PersistedPartyRelationshipState
  case object ToBeValidated extends PersistedPartyRelationshipState

  def fromApi(status: RelationshipState): PersistedPartyRelationshipState = status match {
    case RelationshipState.PENDING       => Pending
    case RelationshipState.ACTIVE        => Active
    case RelationshipState.SUSPENDED     => Suspended
    case RelationshipState.DELETED       => Deleted
    case RelationshipState.REJECTED      => Rejected
    case RelationshipState.TOBEVALIDATED => ToBeValidated
  }
}
