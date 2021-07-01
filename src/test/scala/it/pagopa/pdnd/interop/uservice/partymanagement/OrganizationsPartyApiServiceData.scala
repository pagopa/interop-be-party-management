package it.pagopa.pdnd.interop.uservice.partymanagement

import it.pagopa.pdnd.interop.uservice.partymanagement.model._

object OrganizationsPartyApiServiceData {

  lazy final val uuid1 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9210"
  lazy final val uuid2 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9211"
  lazy final val uuid3 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9212"
  lazy final val uuid4 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9213"
  lazy final val uuid5 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9231"

  lazy final val institutionId1 = "id1"
  lazy final val institutionId2 = "id2"
  lazy final val institutionId3 = "id3"
  lazy final val institutionId4 = "id4"

  lazy final val seed1 = OrganizationSeed(institutionId1, "Institutions One", "John Doe", "mail1@mail.org", Seq.empty)
  lazy final val seed2 =
    OrganizationSeed(institutionId2, "Institutions Two", "Tyler Durden", "mail2@mail.org", Seq.empty)
  lazy final val seed3 =
    OrganizationSeed(institutionId3, "Institutions Three", "Kaiser Soze", "mail3@mail.org", Seq.empty)
  lazy final val seed4 =
    OrganizationSeed(institutionId4, "Institutions Four", "Snake Plissken", "mail4@mail.org", Seq.empty)

  lazy final val expected1 = Organization(
    institutionId = institutionId1,
    description = "Institutions One",
    manager = "John Doe",
    digitalAddress = "mail1@mail.org",
    partyId = uuid1,
    attributes = Seq.empty
  )
  lazy final val expected3 = Organization(
    institutionId = institutionId3,
    description = "Institutions Three",
    manager = "Kaiser Soze",
    digitalAddress = "mail3@mail.org",
    partyId = uuid3,
    attributes = Seq.empty
  )

}
