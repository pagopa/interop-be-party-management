package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.ApiParty
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Organization, OrganizationSeed, Person, PersonSeed}

import java.time.OffsetDateTime
import java.util.UUID

sealed trait Party {
  def id: UUID // TODO probably not necessary
  def externalId: String
  def start: OffsetDateTime
  def end: Option[OffsetDateTime]

}

object Party {

  def convertToApi(party: Party): ApiParty =
    party match {
      case personParty: PersonParty =>
        Right(
          Person(
            name = personParty.name,
            taxCode = personParty.externalId,
            surname = personParty.surname,
            partyId = personParty.id.toString
          )
        )
      case institutionParty: InstitutionParty =>
        Left(
          Organization(
            description = institutionParty.description,
            institutionId = institutionParty.externalId,
            manager = institutionParty.manager,
            digitalAddress = institutionParty.digitalAddress,
            partyId = institutionParty.id.toString
          )
        )
    }

}

final case class PersonParty(
  id: UUID,
  externalId: String,
  name: String,
  surname: String,
  start: OffsetDateTime,
  end: Option[OffsetDateTime]
) extends Party

object PersonParty {
  def fromApi(person: PersonSeed): PersonParty = PersonParty(
    id = UUID.randomUUID(),
    externalId = person.taxCode,
    name = person.name,
    surname = person.surname,
    start = OffsetDateTime.now(),
    end = None
  )
}

final case class InstitutionParty(
  id: UUID,
  externalId: String,
  description: String,
  digitalAddress: String,
  manager: String,
  start: OffsetDateTime,
  end: Option[OffsetDateTime]
) extends Party

object InstitutionParty {
  def fromApi(organization: OrganizationSeed): InstitutionParty = InstitutionParty(
    id = UUID.randomUUID(),
    externalId = organization.institutionId,
    description = organization.description,
    digitalAddress = organization.digitalAddress,
    manager = organization.manager,
    start = OffsetDateTime.now(),
    end = None
  )
}
