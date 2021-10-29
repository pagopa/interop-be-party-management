package it.pagopa.pdnd.interop.uservice.partymanagement.model

/** Represents the party relationship status
  */

sealed trait RelationshipStatusEnum

case object PENDING   extends RelationshipStatusEnum
case object ACTIVE    extends RelationshipStatusEnum
case object SUSPENDED extends RelationshipStatusEnum
case object DELETED   extends RelationshipStatusEnum
case object REJECTED  extends RelationshipStatusEnum

object RelationshipStatusEnum {
  import spray.json._

  implicit object RelationshipStatusEnumFormat extends RootJsonFormat[RelationshipStatusEnum] {
    def write(obj: RelationshipStatusEnum): JsValue =
      obj match {
        case PENDING   => JsString("PENDING")
        case ACTIVE    => JsString("ACTIVE")
        case SUSPENDED => JsString("SUSPENDED")
        case DELETED   => JsString("DELETED")
        case REJECTED  => JsString("REJECTED")
      }

    def read(json: JsValue): RelationshipStatusEnum =
      json match {
        case JsString("PENDING")   => PENDING
        case JsString("ACTIVE")    => ACTIVE
        case JsString("SUSPENDED") => SUSPENDED
        case JsString("DELETED")   => DELETED
        case JsString("REJECTED")  => REJECTED
        case unrecognized =>
          deserializationError(s"RelationshipStatusEnum serialization error ${unrecognized.toString}")
      }
  }

  def fromValue(value: String): Either[Throwable, RelationshipStatusEnum] =
    value match {
      case "PENDING"   => Right(PENDING)
      case "ACTIVE"    => Right(ACTIVE)
      case "SUSPENDED" => Right(SUSPENDED)
      case "DELETED"   => Right(DELETED)
      case "REJECTED"  => Right(REJECTED)
      case other       => Left(new RuntimeException(s"Unable to decode value $other"))
    }
}
