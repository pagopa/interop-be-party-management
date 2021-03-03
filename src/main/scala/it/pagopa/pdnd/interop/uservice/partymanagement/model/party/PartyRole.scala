package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

sealed trait PartyRole {
  def stringify: String = this match {
    case DelegatedBy => "DelegatedBy"
    case ManagerOf   => "ManagerOf"
    case PartOf      => "PartOf"
  }
}

case object DelegatedBy extends PartyRole

case object ManagerOf extends PartyRole

case object PartOf extends PartyRole

object PartyRole {
  def apply(str: String): Either[Throwable, PartyRole] = str match {
    case "DelegatedBy" => Right(DelegatedBy)
    case "ManagerOf"   => Right(ManagerOf)
    case "PartOf"      => Right(PartOf)
    case _             => Left(new RuntimeException("Invalid PartyRole")) //TODO meaningful error
  }
}
