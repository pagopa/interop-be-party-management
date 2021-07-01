package it.pagopa.pdnd.interop.uservice.partymanagement

import it.pagopa.pdnd.interop.uservice.partymanagement.model._

object RelationshipPartyApiServiceData {

  lazy final val uuid0 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9215"
  lazy final val uuid1 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9219"
  lazy final val uuid2 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9220"
  lazy final val uuid3 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9221"
  lazy final val uuid4 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9222"
  lazy final val uuid5 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9223"
  lazy final val uuid6 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9224"

  lazy final val taxCode1 = "RSSMRA75L01H501E"
  lazy final val taxCode2 = "RSSMRA75L01H501F"
  lazy final val taxCode3 = "RSSMRA75L01H501G"

  lazy final val institutionId1 = "id5"
  lazy final val institutionId2 = "id6"
  lazy final val institutionId3 = "id7"

  lazy final val personSeed1 = PersonSeed(taxCode = taxCode1, surname = "Ripley", name = "Ellen")
  lazy final val personSeed2 = PersonSeed(taxCode = taxCode2, surname = "Onizuka", name = "Eikichi")
  lazy final val personSeed3 = PersonSeed(taxCode = taxCode3, surname = "Murphy", name = "Alex")

  lazy final val organizationSeed1 =
    OrganizationSeed(institutionId1, "Institutions Five", "Ellen Ripley", "mail5@mail.org", Seq.empty)
  lazy final val organizationSeed2 =
    OrganizationSeed(institutionId2, "Institutions Six", "Eikichi Onizuka", "mail6@mail.org", Seq.empty)
  lazy final val organizationSeed3 =
    OrganizationSeed(institutionId3, "Institutions Seven", "Alex Murphy", "mail7@mail.org", Seq.empty)

  lazy final val seed1 = RelationShip(from = taxCode1, to = institutionId1, role = "Manager", None)
  lazy final val seed2 = RelationShip(from = taxCode2, to = institutionId2, role = "Manager", None)
  lazy final val seed3 = RelationShip(from = taxCode3, to = institutionId3, role = "Manager", None)

  lazy final val expected0 = RelationShips(Seq.empty)
  lazy final val expected1 = RelationShips(
    Seq(RelationShip(from = taxCode2, to = institutionId2, role = "Manager", status = Some("Pending")))
  )
}
