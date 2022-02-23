package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.partymanagement.model.PartyRole
import spray.json.DefaultJsonProtocol

sealed trait PersistedPartyRole {
  def toApi: PartyRole = this match {
    case PersistedPartyRole.Manager     => PartyRole.MANAGER
    case PersistedPartyRole.Delegate    => PartyRole.DELEGATE
    case PersistedPartyRole.SubDelegate => PartyRole.SUB_DELEGATE
    case PersistedPartyRole.Operator    => PartyRole.OPERATOR
  }
}

object PersistedPartyRole extends DefaultJsonProtocol {

  case object Manager     extends PersistedPartyRole
  case object Delegate    extends PersistedPartyRole
  case object SubDelegate extends PersistedPartyRole
  case object Operator    extends PersistedPartyRole

  def fromApi(role: PartyRole): PersistedPartyRole = role match {
    case PartyRole.MANAGER      => Manager
    case PartyRole.DELEGATE     => Delegate
    case PartyRole.SUB_DELEGATE => SubDelegate
    case PartyRole.OPERATOR     => Operator
  }
}
