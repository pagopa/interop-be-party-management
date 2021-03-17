package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.ApiParty
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Organization, Person}

import java.time.OffsetDateTime
import java.util.UUID

sealed trait Party {
  def id: UUID
  def externalId: String
  def `type`: Option[PartyType]
  def start: OffsetDateTime
  def end: Option[OffsetDateTime]

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
          Organization(
            name = institutionParty.name,
            phone = institutionParty.phone,
            email = institutionParty.email,
            institutionId = institutionParty.externalId,
            manager = institutionParty.manager,
            digitalAddress = institutionParty.digitalAddress
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
        end = None
      )
    case Left(organization: Organization) =>
      InstitutionParty(
        id = UUID.randomUUID(),
        externalId = organization.institutionId,
        name = organization.name,
        email = organization.email,
        phone = organization.phone,
        digitalAddress = organization.digitalAddress,
        manager = organization.manager,
        `type` = None,
        start = OffsetDateTime.now(),
        end = None
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
  end: Option[OffsetDateTime]
) extends Party

final case class InstitutionParty(
  id: UUID,
  externalId: String,
  name: String,
  email: Option[String],
  phone: Option[String],
  digitalAddress: String,
  manager: String,
  `type`: Option[PartyType],
  start: OffsetDateTime,
  end: Option[OffsetDateTime]
) extends Party
