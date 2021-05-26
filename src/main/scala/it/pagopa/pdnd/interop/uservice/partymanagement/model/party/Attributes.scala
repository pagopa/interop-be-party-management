package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.model.AttributeRecord

final case class Attributes(kind: String, values: Set[String]) {
  def toApi: AttributeRecord = AttributeRecord(kind, values.toSeq)
}

object Attributes {
  def fromApi(attributeRecord: AttributeRecord): Attributes =
    Attributes(attributeRecord.kind, attributeRecord.attributes.toSet)
}
