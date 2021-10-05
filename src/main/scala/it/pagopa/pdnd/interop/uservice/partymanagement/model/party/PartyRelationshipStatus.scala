package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

sealed trait PartyRelationshipStatus {
  def stringify: String = this match {
    case PartyRelationshipStatus.Pending   => "Pending"
    case PartyRelationshipStatus.Active    => "Active"
    case PartyRelationshipStatus.Suspended => "Suspended"
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
object PartyRelationshipStatus {

  case object Pending   extends PartyRelationshipStatus
  case object Active    extends PartyRelationshipStatus
  case object Suspended extends PartyRelationshipStatus

  def fromText(str: String): Either[Throwable, PartyRelationshipStatus] = str match {
    case "Pending"   => Right(Pending)
    case "Active"    => Right(Active)
    case "Suspended" => Right(Suspended)
    case _           => Left(new RuntimeException("Deserialization from protobuf failed"))
  }
}
