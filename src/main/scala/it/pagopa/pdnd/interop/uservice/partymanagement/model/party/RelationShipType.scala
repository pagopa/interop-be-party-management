package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

sealed trait RelationShipType {
  def stringify: String = this match {
    case DelegatedBy => "DelegatedBy"
    case ManagerOf   => "ManagerOf"
    case PartOf      => "PartOf"
  }
}

case object DelegatedBy extends RelationShipType

case object ManagerOf extends RelationShipType

case object PartOf extends RelationShipType
