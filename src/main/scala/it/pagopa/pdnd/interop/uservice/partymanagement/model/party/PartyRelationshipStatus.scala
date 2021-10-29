package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.model._

sealed trait PartyRelationshipStatus {
//  def stringify: String = this match {
//    case PartyRelationshipStatus.Pending   => "Pending"
//    case PartyRelationshipStatus.Active    => "Active"
//    case PartyRelationshipStatus.Suspended => "Suspended"
//    case PartyRelationshipStatus.Deleted   => "Deleted"
//    case PartyRelationshipStatus.Rejected  => "Rejected"
//  }
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

//  def fromText(str: String): Either[Throwable, PartyRelationshipStatus] = str match {
//    case "Pending"   => Right(Pending)
//    case "Active"    => Right(Active)
//    case "Suspended" => Right(Suspended)
//    case "Deleted"   => Right(Deleted)
//    case "Rejected"  => Right(Rejected)
//    case _           => Left(new RuntimeException("Deserialization from protobuf failed"))
//  }

  def fromApi(status: RelationshipStatusEnum): PartyRelationshipStatus = status match {
    case PENDING   => Pending
    case ACTIVE    => Active
    case SUSPENDED => Suspended
    case DELETED   => Deleted
    case REJECTED  => Rejected
  }
}
