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

sealed trait PersonType         extends PartyType
case object Operator            extends PersonType
case object LegalRepresentative extends PersonType
case object Delegate            extends PersonType
case object GroupLeader         extends PersonType

sealed trait OrganizationType extends PartyType
case object AAOU              extends OrganizationType
case object UO                extends OrganizationType
