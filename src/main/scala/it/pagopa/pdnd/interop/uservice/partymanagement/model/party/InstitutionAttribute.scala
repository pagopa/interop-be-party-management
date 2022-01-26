package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.model.Attribute

final case class InstitutionAttribute(origin: String, code: String)

object InstitutionAttribute {
  def toApi(attribute: InstitutionAttribute): Attribute   = Attribute(attribute.origin, attribute.code)
  def fromApi(attribute: Attribute): InstitutionAttribute = InstitutionAttribute(attribute.origin, attribute.code)
}
