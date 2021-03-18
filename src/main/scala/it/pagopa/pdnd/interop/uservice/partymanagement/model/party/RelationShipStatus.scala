package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

sealed trait RelationShipStatus {
  def stringify: String = this match {
    case RelationShipStatus.Pending  => "Pending"
    case RelationShipStatus.Active   => "Active"
    case RelationShipStatus.Inactive => "Inactive"
    case RelationShipStatus.Deleted  => "Deleted"
  }
}

object RelationShipStatus {

  case object Pending  extends RelationShipStatus
  case object Active   extends RelationShipStatus
  case object Inactive extends RelationShipStatus
  case object Deleted  extends RelationShipStatus

  def apply(str: String): Either[Throwable, RelationShipStatus] = str match {
    case "Pending"  => Right(Pending)
    case "Active"   => Right(Active)
    case "Inactive" => Right(Inactive)
    case "Deleted"  => Right(Deleted)
    case _          => Left(new RuntimeException("Invalid party status")) //TODO meaningful error
  }
}
