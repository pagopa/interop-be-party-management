package it.pagopa.pdnd.interop.uservice.partymanagement

import it.pagopa.pdnd.interop.uservice.partymanagement.model._

object PersonPartyApiServiceData {

  final lazy val uuid1 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9215"
  final lazy val uuid2 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9216"
  final lazy val uuid3 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9217"
  final lazy val uuid4 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9218"
  final lazy val uuid5 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9281"

  final lazy val taxCode1 = "RSSMRA75L01H501A"
  final lazy val taxCode2 = "RSSMRA75L01H501B"
  final lazy val taxCode3 = "RSSMRA75L01H501C"
  final lazy val taxCode4 = "RSSMRA75L01H501D"

  final lazy val seed1 = PersonSeed(taxCode = taxCode1, surname = "Doe", name = "Joe")
  final lazy val seed2 = PersonSeed(taxCode = taxCode2, surname = "Durden", name = "Tyler")
  final lazy val seed3 = PersonSeed(taxCode = taxCode3, surname = "Soze", name = "Keiser")
  final lazy val seed4 = PersonSeed(taxCode = taxCode4, surname = "Plissken", name = "Snake")

  final lazy val expected1 = Person(taxCode = taxCode1, surname = "Doe", name = "Joe", partyId = uuid1)
  final lazy val expected2 = Person(taxCode = taxCode3, surname = "Soze", name = "Keiser", partyId = uuid3)

}
