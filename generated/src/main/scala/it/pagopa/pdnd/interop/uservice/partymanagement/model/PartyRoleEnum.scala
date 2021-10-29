package it.pagopa.pdnd.interop.uservice.partymanagement.model

/** Represents the generic available role types for the relationship
  */

sealed trait PartyRoleEnum

case object MANAGER  extends PartyRoleEnum
case object DELEGATE extends PartyRoleEnum
case object OPERATOR extends PartyRoleEnum

object PartyRoleEnum {
  import spray.json._

  implicit object PartyRoleEnumFormat extends RootJsonFormat[PartyRoleEnum] {
    def write(obj: PartyRoleEnum): JsValue =
      obj match {
        case MANAGER  => JsString("MANAGER")
        case DELEGATE => JsString("DELEGATE")
        case OPERATOR => JsString("OPERATOR")
      }

    def read(json: JsValue): PartyRoleEnum =
      json match {
        case JsString("MANAGER")  => MANAGER
        case JsString("DELEGATE") => DELEGATE
        case JsString("OPERATOR") => OPERATOR
        case unrecognized         => deserializationError(s"PartyRoleEnum serialization error ${unrecognized.toString}")
      }
  }

  def fromValue(value: String): Either[Throwable, PartyRoleEnum] =
    value match {
      case "MANAGER"  => Right(MANAGER)
      case "DELEGATE" => Right(DELEGATE)
      case "OPERATOR" => Right(OPERATOR)
      case other      => Left(new RuntimeException(s"Unable to decode value $other"))
    }
}
