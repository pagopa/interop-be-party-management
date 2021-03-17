package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import scala.concurrent.Future

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

object PartyRole {
  def fromText(str: String): Future[PartyRole] = str match {
    case "Manager"  => Future.successful(Manager)
    case "Delegate" => Future.successful(Delegate)
    case "Operator" => Future.successful(Operator)
    case _          => Future.failed(new RuntimeException("Invalid PartyRole")) //TODO meaningful error
  }
}
