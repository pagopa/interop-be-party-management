package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

sealed trait PartyRelationshipStatus {
  def stringify: String = this match {
    case PartyRelationshipStatus.Pending   => "Pending"
    case PartyRelationshipStatus.Active    => "Active"
    case PartyRelationshipStatus.Suspended => "Suspended"
    case PartyRelationshipStatus.Deleted   => "Deleted"
    case PartyRelationshipStatus.Rejected  => "Rejected"
  }
}

object PartyRelationshipStatus {

  case object Pending   extends PartyRelationshipStatus
  case object Active    extends PartyRelationshipStatus
  case object Suspended extends PartyRelationshipStatus
  case object Deleted   extends PartyRelationshipStatus
  case object Rejected  extends PartyRelationshipStatus

  def fromText(str: String): Either[Throwable, PartyRelationshipStatus] = str match {
    case "Pending"   => Right(Pending)
    case "Active"    => Right(Active)
    case "Suspended" => Right(Suspended)
    case "Deleted"   => Right(Deleted)
    case "Rejected"  => Right(Rejected)
    case _           => Left(new RuntimeException("Deserialization from protobuf failed"))
  }
}
