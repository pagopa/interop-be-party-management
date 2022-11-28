package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.partymanagement.model.GeographicTaxonomy

final case class PersistedGeographicTaxonomy(code: String, desc: String)

object PersistedGeographicTaxonomy {
  def toApi(geographicTaxonomy: PersistedGeographicTaxonomy): GeographicTaxonomy =
    GeographicTaxonomy(code = geographicTaxonomy.code, desc = geographicTaxonomy.desc)

  def fromApi(geographicTaxonomy: GeographicTaxonomy): PersistedGeographicTaxonomy =
    PersistedGeographicTaxonomy(code = geographicTaxonomy.code, desc = geographicTaxonomy.desc)
}
