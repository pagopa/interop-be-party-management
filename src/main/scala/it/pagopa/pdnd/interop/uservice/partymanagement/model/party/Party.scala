package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.ApiParty
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.Converter
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.Converter.Aux
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Institution, Person}

import java.time.OffsetDateTime
import java.util.UUID

sealed trait Party {
  def id: UUID
  def `type`: Option[PartyType]
  def start: OffsetDateTime
  def end: Option[OffsetDateTime]
  def status: PartyStatus
}

object Party {
  implicit def convertFromApi: Aux[ApiParty, Party] =
    new Converter[ApiParty] {
      type B = Party
      def value(apiParty: ApiParty): Party = apiParty match {
        case Right(person: Person) =>
          PersonParty(
            id = person.id,
            name = person.name,
            surname = person.surname,
            email = person.email,
            phone = person.phone,
            taxCode = person.taxCode,
            `type` = None,
            start = person.start,
            end = person.end,
            status = Pending
          )
        case Left(institution: Institution) =>
          InstitutionParty(
            id = institution.id,
            ipaCod = institution.ipaCod,
            name = institution.name,
            email = institution.email,
            phone = institution.phone,
            pec = institution.pec,
            manager = institution.manager,
            taxCode = institution.taxCode,
            `type` = None,
            start = institution.start,
            end = institution.end,
            status = Pending //TODO probably it never be Pending
          )
      }
    }

  implicit def convertToApi: Aux[Party, ApiParty] =
    new Converter[Party] {
      type B = ApiParty
      def value(party: Party): ApiParty = party match {
        case personParty: PersonParty =>
          Right {
            Person(
              id = personParty.id,
              name = personParty.name,
              phone = personParty.phone,
              email = personParty.email,
              taxCode = personParty.taxCode,
              start = personParty.start,
              end = personParty.end,
              surname = personParty.surname
            )
          }
        case institutionParty: InstitutionParty =>
          Left {
            Institution(
              id = institutionParty.id,
              name = institutionParty.name,
              phone = institutionParty.phone,
              email = institutionParty.email,
              taxCode = institutionParty.taxCode,
              start = institutionParty.start,
              end = institutionParty.end,
              ipaCod = institutionParty.ipaCod,
              manager = institutionParty.manager,
              pec = institutionParty.pec
            )
          }
      }
    }
}

final case class PersonParty(
  id: UUID,
  name: String,
  surname: String,
  email: Option[String],
  phone: Option[String],
  taxCode: String,
  `type`: Option[PartyType],
  start: OffsetDateTime,
  end: Option[OffsetDateTime],
  status: PartyStatus
) extends Party

final case class InstitutionParty(
  id: UUID,
  ipaCod: String,
  name: String,
  email: Option[String],
  phone: Option[String],
  pec: String,
  manager: String,
  taxCode: String,
  `type`: Option[PartyType],
  start: OffsetDateTime,
  end: Option[OffsetDateTime],
  status: PartyStatus
) extends Party
