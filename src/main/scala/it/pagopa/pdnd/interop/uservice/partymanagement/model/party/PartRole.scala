package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

sealed trait PartRole {
  def stringify: String = this match {
    case DelegatedBy => "DelegatedBy"
    case ManagerOf   => "ManagerOf"
    case PartOf      => "PartOf"
  }
}

case object DelegatedBy extends PartRole

case object ManagerOf extends PartRole

case object PartOf extends PartRole

object PartRole {
  def apply(str: String): Either[Throwable, PartRole] = str match {
    case "DelegatedBy" => Right(DelegatedBy)
    case "ManagerOf"   => Right(ManagerOf)
    case "PartOf"      => Right(PartOf)
    case _             => Left(new RuntimeException) //TODO meaningful error
  }
}
