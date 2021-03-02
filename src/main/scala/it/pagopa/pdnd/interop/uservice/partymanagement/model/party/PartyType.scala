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

sealed trait InstitutionType extends PartyType
case object AAOU             extends InstitutionType
case object UO               extends InstitutionType

object PartyType {
  def apply(str: String): Either[Throwable, PartyType] = str match {
    case "Operator"            => Right(Operator)
    case "LegalRepresentative" => Right(LegalRepresentative)
    case "Delegate"            => Right(Delegate)
    case "GroupLeader"         => Right(GroupLeader)
    case "AAOU"                => Right(AAOU)
    case "UO"                  => Right(UO)
    case _                     => Left(new RuntimeException) //TODO meaningful error
  }
}
