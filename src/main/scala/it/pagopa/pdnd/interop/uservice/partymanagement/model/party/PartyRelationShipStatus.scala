package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

sealed trait PartyRelationShipStatus {
  def stringify: String = this match {
    case PartyRelationShipStatus.Pending  => "Pending"
    case PartyRelationShipStatus.Active   => "Active"
    case PartyRelationShipStatus.Inactive => "Inactive"
    case PartyRelationShipStatus.Deleted  => "Deleted"
  }
}
@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.Equals",
    "org.wartremover.warts.ToString"
  )
)
object PartyRelationShipStatus {

  case object Pending  extends PartyRelationShipStatus
  case object Active   extends PartyRelationShipStatus
  case object Inactive extends PartyRelationShipStatus
  case object Deleted  extends PartyRelationShipStatus

  def fromText(str: String): Either[Throwable, PartyRelationShipStatus] = str match {
    case "Pending"  => Right(Pending)
    case "Active"   => Right(Active)
    case "Inactive" => Right(Inactive)
    case "Deleted"  => Right(Deleted)
    case _          => Left(new RuntimeException("Deserialization from protobuf failed"))
  }
}
