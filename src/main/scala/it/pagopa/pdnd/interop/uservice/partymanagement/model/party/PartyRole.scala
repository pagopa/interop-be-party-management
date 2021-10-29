package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.model.{DELEGATE, MANAGER, OPERATOR, PartyRoleEnum}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, deserializationError}

import scala.util.{Failure, Success, Try}

sealed trait PartyRole {
  def toApi: PartyRoleEnum = this match {
    case Manager  => MANAGER
    case Delegate => DELEGATE
    case Operator => OPERATOR
  }
}

case object Delegate extends PartyRole

case object Manager extends PartyRole

case object Operator extends PartyRole

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

  def fromApi(role: PartyRoleEnum): PartyRole = role match {
    case MANAGER  => Manager
    case DELEGATE => Delegate
    case OPERATOR => Operator
  }
}
