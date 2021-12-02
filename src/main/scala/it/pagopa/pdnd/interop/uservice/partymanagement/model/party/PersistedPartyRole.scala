package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.model.PartyRole
import spray.json.DefaultJsonProtocol

sealed trait PersistedPartyRole {
  def toApi: PartyRole = this match {
    case Manager     => PartyRole.MANAGER
    case Delegate    => PartyRole.DELEGATE
    case SubDelegate => PartyRole.SUB_DELEGATE
    case Operator    => PartyRole.OPERATOR
  }
}

case object Manager     extends PersistedPartyRole
case object Delegate    extends PersistedPartyRole
case object SubDelegate extends PersistedPartyRole
case object Operator    extends PersistedPartyRole

object PersistedPartyRole extends DefaultJsonProtocol {

  def fromApi(role: PartyRole): PersistedPartyRole = role match {
    case PartyRole.MANAGER      => Manager
    case PartyRole.DELEGATE     => Delegate
    case PartyRole.SUB_DELEGATE => SubDelegate
    case PartyRole.OPERATOR     => Operator
  }
}
