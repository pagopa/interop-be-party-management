package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

sealed trait PartyStatus {
  def stringify: String = this match {
    case Pending  => "Pending"
    case Active   => "Active"
    case Inactive => "Inactive"
    case Deleted  => "Deleted"
  }
}

case object Pending  extends PartyStatus
case object Active   extends PartyStatus
case object Inactive extends PartyStatus
case object Deleted  extends PartyStatus

object PartyStatus {
  def apply(str: String): Either[Throwable, PartyStatus] = str match {
    case "Pending"  => Right(Pending)
    case "Active"   => Right(Active)
    case "Inactive" => Right(Inactive)
    case "Deleted"  => Right(Deleted)
    case _          => Left(new RuntimeException("Invalid party status")) //TODO meaningful error
  }
}
