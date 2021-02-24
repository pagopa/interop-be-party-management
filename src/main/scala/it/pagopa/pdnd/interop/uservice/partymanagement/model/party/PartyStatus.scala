package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

sealed trait PartyStatus {
  def stringify: String = this match {
    case Active   => "Active"
    case Inactive => "Inactive"
    case Deleted  => "Deleted"
  }
}

case object Active extends PartyStatus

case object Inactive extends PartyStatus

case object Deleted extends PartyStatus