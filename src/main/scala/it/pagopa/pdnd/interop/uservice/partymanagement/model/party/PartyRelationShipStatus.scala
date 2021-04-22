package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

sealed trait PartyRelationShipStatus {
  def stringify: String = this match {
    case PartyRelationShipStatus.Pending  => "Pending"
    case PartyRelationShipStatus.Active   => "Active"
    case PartyRelationShipStatus.Inactive => "Inactive"
    case PartyRelationShipStatus.Deleted  => "Deleted"
  }
}

object PartyRelationShipStatus {

  case object Pending  extends PartyRelationShipStatus
  case object Active   extends PartyRelationShipStatus
  case object Inactive extends PartyRelationShipStatus
  case object Deleted  extends PartyRelationShipStatus

  def apply(str: String): PartyRelationShipStatus = str match {
    case "Pending"  => Pending
    case "Active"   => Active
    case "Inactive" => Inactive
    case "Deleted"  => Deleted
  }
}
