package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.model._

sealed trait PartyRelationshipStatus {
  def toApi: RelationshipStatusEnum = this match {
    case PartyRelationshipStatus.Pending   => PENDING
    case PartyRelationshipStatus.Active    => ACTIVE
    case PartyRelationshipStatus.Suspended => SUSPENDED
    case PartyRelationshipStatus.Deleted   => DELETED
    case PartyRelationshipStatus.Rejected  => REJECTED
  }
}

object PartyRelationshipStatus {

  case object Pending   extends PartyRelationshipStatus
  case object Active    extends PartyRelationshipStatus
  case object Suspended extends PartyRelationshipStatus
  case object Deleted   extends PartyRelationshipStatus
  case object Rejected  extends PartyRelationshipStatus

  def fromApi(status: RelationshipStatusEnum): PartyRelationshipStatus = status match {
    case PENDING   => Pending
    case ACTIVE    => Active
    case SUSPENDED => Suspended
    case DELETED   => Deleted
    case REJECTED  => Rejected
  }
}
