package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.ApiParty
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Institution, Person}

import java.time.OffsetDateTime
import java.util.UUID

sealed trait Party {
  def id: UUID
  def externalId: String
  def `type`: Option[PartyType]
  def start: OffsetDateTime
  def end: Option[OffsetDateTime]
  def status: PartyStatus

}

object Party {

  def convertToApi(party: Party): ApiParty =
    party match {
      case personParty: PersonParty =>
        Right {
          Person(
            name = personParty.name,
            phone = personParty.phone,
            email = personParty.email,
            taxCode = personParty.externalId,
            surname = personParty.surname
          )
        }
      case institutionParty: InstitutionParty =>
        Left {
          Institution(
            name = institutionParty.name,
            phone = institutionParty.phone,
            email = institutionParty.email,
            taxCode = institutionParty.externalId,
            manager = institutionParty.manager,
            pec = institutionParty.pec
          )
        }
    }

  def createFromApi(apiParty: ApiParty): Party = apiParty match {
    case Right(person: Person) =>
      PersonParty(
        id = UUID.randomUUID(),
        externalId = person.taxCode,
        name = person.name,
        surname = person.surname,
        email = person.email,
        phone = person.phone,
        `type` = None,
        start = OffsetDateTime.now(),
        end = None,
        status = Pending
      )
    case Left(institution: Institution) =>
      InstitutionParty(
        id = UUID.randomUUID(),
        externalId = institution.taxCode,
        name = institution.name,
        email = institution.email,
        phone = institution.phone,
        pec = institution.pec,
        manager = institution.manager,
        `type` = None,
        start = OffsetDateTime.now(),
        end = None,
        status = Active //TODO probably it never be Pending
      )
  }

}

final case class PersonParty(
  id: UUID,
  externalId: String,
  name: String,
  surname: String,
  email: Option[String],
  phone: Option[String],
  `type`: Option[PartyType],
  start: OffsetDateTime,
  end: Option[OffsetDateTime],
  status: PartyStatus
) extends Party

final case class InstitutionParty(
  id: UUID,
  externalId: String,
  name: String,
  email: Option[String],
  phone: Option[String],
  pec: String,
  manager: String,
  `type`: Option[PartyType],
  start: OffsetDateTime,
  end: Option[OffsetDateTime],
  status: PartyStatus
) extends Party
