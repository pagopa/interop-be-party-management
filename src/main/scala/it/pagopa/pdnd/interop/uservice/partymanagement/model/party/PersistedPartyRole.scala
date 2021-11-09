package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.model.PartyRole
import spray.json.DefaultJsonProtocol

sealed trait PersistedPartyRole {
  def toApi: PartyRole = this match {
    case Manager  => PartyRole.MANAGER
    case Delegate => PartyRole.DELEGATE
    case Operator => PartyRole.OPERATOR
  }
}

case object Delegate extends PersistedPartyRole
case object Manager  extends PersistedPartyRole
case object Operator extends PersistedPartyRole

object PersistedPartyRole extends DefaultJsonProtocol {

//  implicit val format: JsonFormat[PartyRole] = new JsonFormat[PartyRole] {
//    override def write(obj: PartyRole): JsValue = obj match {
//      case Manager  => JsString("Manager")
//      case Delegate => JsString("Delegate")
//      case Operator => JsString("Operator")
//    }
//
//    override def read(json: JsValue): PartyRole = json match {
//      case JsString(s) =>
//        val res: Try[PartyRole] = s match {
//          case "Manager"  => Success(Manager)
//          case "Delegate" => Success(Delegate)
//          case "Operator" => Success(Operator)
//          case _          => Failure(new RuntimeException("Invalid token status"))
//        }
//        res.fold(ex => deserializationError(msg = ex.getMessage, cause = ex), identity)
//      case notAJsString =>
//        deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
//    }
//
//  }

  def fromApi(role: PartyRole): PersistedPartyRole = role match {
    case PartyRole.MANAGER  => Manager
    case PartyRole.DELEGATE => Delegate
    case PartyRole.OPERATOR => Operator
  }
}
