package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.partymanagement.model.Attribute

final case class InstitutionAttribute(origin: String, code: String, description: String)

object InstitutionAttribute {
  def toApi(attribute: InstitutionAttribute): Attribute =
    Attribute(origin = attribute.origin, code = attribute.code, description = attribute.description)

  def fromApi(attribute: Attribute): InstitutionAttribute =
    InstitutionAttribute(origin = attribute.origin, code = attribute.code, description = attribute.description)
}
