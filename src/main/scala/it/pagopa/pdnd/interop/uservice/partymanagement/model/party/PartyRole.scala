package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, deserializationError}

import scala.util.{Failure, Success, Try}

sealed trait PartyRole {
  def stringify: String = this match {
    case Manager  => "Manager"
    case Delegate => "Delegate"
    case Operator => "Operator"
  }
}

case object Delegate extends PartyRole

case object Manager extends PartyRole

case object Operator extends PartyRole

@SuppressWarnings(
  Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Equals", "org.wartremover.warts.ToString")
)
object PartyRole extends DefaultJsonProtocol {

  implicit val format: JsonFormat[PartyRole] = new JsonFormat[PartyRole] {
    override def write(obj: PartyRole): JsValue = obj match {
      case Manager  => JsString("Manager")
      case Delegate => JsString("Delegate")
      case Operator => JsString("Operator")
    }

    override def read(json: JsValue): PartyRole = json match {
      case JsString(s) =>
        val res: Try[PartyRole] = s match {
          case "Manager"  => Success(Manager)
          case "Delegate" => Success(Delegate)
          case "Operator" => Success(Operator)
          case _          => Failure(new RuntimeException("Invalid token status"))
        }
        res.fold(ex => deserializationError(msg = ex.getMessage, cause = ex), identity)
      case notAJsString =>
        deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
    }

  }

  def fromText(str: String): Either[Throwable, PartyRole] = str match {
    case "Manager"  => Right(Manager)
    case "Delegate" => Right(Delegate)
    case "Operator" => Right(Operator)
    case _          => Left(new RuntimeException("Invalid PartyRole")) //TODO meaningful error
  }
}
