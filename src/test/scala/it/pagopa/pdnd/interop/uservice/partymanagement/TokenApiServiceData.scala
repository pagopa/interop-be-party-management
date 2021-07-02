package it.pagopa.pdnd.interop.uservice.partymanagement

import it.pagopa.pdnd.interop.uservice.partymanagement.model._

object TokenApiServiceData {

  lazy final val uuid0 = "37f8dce0-0a5b-476b-9fdd-a7a658eb9210"
  lazy final val uuid1 = "37f8dce0-0a5b-476b-9fdd-a7a658eb9211"

  lazy final val tokenSeed1 = "47f8dce0-0a5b-476b-9fdd-a7a658eb9210"

  lazy final val taxCode1 = "RSSMRA75L01H501E"

  lazy final val personSeed1 = PersonSeed(taxCode = taxCode1, surname = "Mascetti", name = "Raffaello")

  lazy final val institutionId1 = "id9"

  lazy final val organizationSeed1 =
    OrganizationSeed(institutionId1, "Institutions Nine", "Raffaello Mascetti", "mail9@mail.org", Seq.empty)

  lazy final val seed1 = RelationShip(from = taxCode1, to = institutionId1, role = "Manager", None)
  lazy final val seed2 = RelationShip(from = taxCode1, to = institutionId1, role = "Delegate", None)

}
