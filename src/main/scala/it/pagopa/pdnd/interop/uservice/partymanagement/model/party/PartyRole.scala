package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import scala.concurrent.Future

sealed trait PartyRole {
  def stringify: String = this match {
    case DelegatedBy => "DelegatedBy"
    case ManagerOf   => "ManagerOf"
    case PartOf      => "PartOf"
  }
}

case object DelegatedBy extends PartyRole

case object ManagerOf extends PartyRole

case object PartOf extends PartyRole

object PartyRole {
  def fromText(str: String): Future[PartyRole] = str match {
    case "DelegatedBy" => Future.successful(DelegatedBy)
    case "ManagerOf"   => Future.successful(ManagerOf)
    case "PartOf"      => Future.successful(PartOf)
    case _             => Future.failed(new RuntimeException("Invalid PartyRole")) //TODO meaningful error
  }
}
