package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.ApiParty
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Organization, OrganizationSeed, Person, PersonSeed}
import it.pagopa.pdnd.interop.uservice.partymanagement.service.UUIDSupplier

import java.time.OffsetDateTime
import java.util.UUID

sealed trait Party {
  def id: UUID
  def start: OffsetDateTime
  def end: Option[OffsetDateTime]

  def addAttributes(attributes: Set[String]): Either[Throwable, Party] = this match {
    case _: PersonParty => Left(new RuntimeException("Attributes do not exist for person party"))
    case institutionParty: InstitutionParty =>
      val updated: Set[String] = institutionParty.attributes ++ attributes
      Right(institutionParty.copy(attributes = updated))

  }

}

object Party {

  def convertToApi(party: Party): ApiParty =
    party match {
      case personParty: PersonParty =>
        Right(Person(id = personParty.id))
      case institutionParty: InstitutionParty =>
        Left(
          Organization(
            description = institutionParty.description,
            institutionId = institutionParty.externalId,
            digitalAddress = institutionParty.digitalAddress,
            id = institutionParty.id,
            attributes = institutionParty.attributes.toSeq
          )
        )
    }

}

final case class PersonParty(id: UUID, start: OffsetDateTime, end: Option[OffsetDateTime]) extends Party

object PersonParty {
  def fromApi(person: PersonSeed): PersonParty = PersonParty(id = person.id, start = OffsetDateTime.now(), end = None)
}

final case class InstitutionParty(
  id: UUID,
  externalId: String,
  description: String,
  digitalAddress: String,
  attributes: Set[String],
  start: OffsetDateTime,
  end: Option[OffsetDateTime]
) extends Party

object InstitutionParty {
  def fromApi(organization: OrganizationSeed, uuidSupplier: UUIDSupplier): InstitutionParty = {
    InstitutionParty(
      id = uuidSupplier.get,
      externalId = organization.institutionId,
      description = organization.description,
      digitalAddress = organization.digitalAddress,
      attributes = organization.attributes.toSet,
      start = OffsetDateTime.now(),
      end = None
    )
  }
}
