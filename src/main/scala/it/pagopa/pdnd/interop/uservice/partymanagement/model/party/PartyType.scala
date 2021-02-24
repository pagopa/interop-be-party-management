package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

sealed trait PartyType {
  def stringify: String = this match {
    case Operator            => "Operator"
    case LegalRepresentative => "LegalRepresentative"
    case Delegate            => "Delegate"
    case GroupLeader         => "GroupLeader"
    case AAOU                => "AAOU"
    case UO                  => "UO"
  }
}

sealed trait Person             extends PartyType
case object Operator            extends Person
case object LegalRepresentative extends Person
case object Delegate            extends Person
case object GroupLeader         extends Person

sealed trait Organization extends PartyType
case object AAOU          extends Organization
case object UO            extends Organization
