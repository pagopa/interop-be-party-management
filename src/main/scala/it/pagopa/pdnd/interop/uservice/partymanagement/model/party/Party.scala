package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Institution, Person}

import java.time.OffsetDateTime
import java.util.UUID

sealed trait Party {
  def id: UUID
  def `type`: PartyType
  def start: OffsetDateTime
  def end: Option[OffsetDateTime]
  def status: PartyStatus
}

final case class PersonParty(
  id: UUID,
  name: String,
  surname: String,
  email: Option[String],
  phone: Option[String],
  taxCode: String,
  `type`: PartyType,
  start: OffsetDateTime,
  end: Option[OffsetDateTime],
  status: PartyStatus
) extends Party {
  def toApi: Person =
    Person(
      id = id,
      name = name,
      phone = phone,
      email = email,
      `type` = `type`.stringify,
      taxCode = taxCode,
      start = start,
      end = end,
      status = status.stringify,
      surname = surname
    )
}

final case class InstitutionParty(
  id: UUID,
  ipaCod: String,
  name: String,
  email: Option[String],
  phone: Option[String],
  pec: String,
  manager: String,
  taxCode: String,
  `type`: PartyType,
  start: OffsetDateTime,
  end: Option[OffsetDateTime],
  status: PartyStatus
) extends Party {
  def toApi: Institution =
    Institution(
      id = id,
      name = name,
      phone = phone,
      email = email,
      `type` = `type`.stringify,
      taxCode = taxCode,
      start = start,
      end = end,
      status = status.stringify,
      ipaCod = ipaCod,
      manager = manager,
      pec = pec
    )
}

object Party {
  def apply(person: Person): Either[Throwable, Party] =
    for {
      partyType <- PartyType(person.`type`)
      status    <- PartyStatus(person.status)
    } yield PersonParty(
      id = person.id,
      name = person.name,
      surname = person.surname,
      email = person.email,
      phone = person.phone,
      taxCode = person.taxCode,
      `type` = partyType,
      start = person.start,
      end = person.end,
      status = status
    )

  def apply(institution: Institution): Either[Throwable, Party] =
    for {
      partyType <- PartyType(institution.`type`)
      status    <- PartyStatus(institution.status)
    } yield InstitutionParty(
      id = institution.id,
      ipaCod = institution.ipaCod,
      name = institution.name,
      email = institution.email,
      phone = institution.phone,
      pec = institution.pec,
      manager = institution.manager,
      taxCode = institution.taxCode,
      `type` = partyType,
      start = institution.start,
      end = institution.end,
      status = status
    )
}
