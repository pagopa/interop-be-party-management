package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.commons.utils.service.UUIDSupplier
import it.pagopa.interop.partymanagement.common.system.ApiParty
import it.pagopa.interop.partymanagement.model.{Attribute, Organization, OrganizationSeed, Person, PersonSeed}
import it.pagopa.interop.partymanagement.service.OffsetDateTimeSupplier

import java.time.OffsetDateTime
import java.util.UUID

sealed trait Party {
  def id: UUID
  def start: OffsetDateTime
  def end: Option[OffsetDateTime]

  def addAttributes(attributes: Set[Attribute]): Either[Throwable, Party] = this match {
    case _: PersonParty => Left(new RuntimeException("Attributes do not exist for person party"))
    case institutionParty: InstitutionParty =>
      val updated: Set[InstitutionAttribute] = institutionParty.attributes ++ attributes.map(attribute =>
        InstitutionAttribute(origin = attribute.origin, code = attribute.code, description = attribute.description)
      )
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
            id = institutionParty.id,
            institutionId = institutionParty.externalId,
            description = institutionParty.description,
            digitalAddress = institutionParty.digitalAddress,
            taxCode = institutionParty.taxCode,
            address = institutionParty.address,
            zipCode = institutionParty.zipCode,
            attributes = institutionParty.attributes.map(InstitutionAttribute.toApi).toSeq
          )
        )
    }
}

final case class PersonParty(id: UUID, start: OffsetDateTime, end: Option[OffsetDateTime]) extends Party

object PersonParty {
  def fromApi(person: PersonSeed, offsetDateTimeSupplier: OffsetDateTimeSupplier): PersonParty =
    PersonParty(id = person.id, start = offsetDateTimeSupplier.get, end = None)
}

final case class InstitutionParty(
  id: UUID,
  externalId: String,
  description: String,
  digitalAddress: String,
  taxCode: String,
  address: String,
  zipCode: String,
  start: OffsetDateTime,
  end: Option[OffsetDateTime],
  attributes: Set[InstitutionAttribute]
) extends Party

object InstitutionParty {
  def fromApi(
    organization: OrganizationSeed,
    uuidSupplier: UUIDSupplier,
    offsetDateTimeSupplier: OffsetDateTimeSupplier
  ): InstitutionParty = {
    InstitutionParty(
      id = uuidSupplier.get,
      externalId = organization.institutionId,
      description = organization.description,
      digitalAddress = organization.digitalAddress,
      taxCode = organization.taxCode,
      address = organization.address,
      zipCode = organization.zipCode,
      attributes = organization.attributes.map(InstitutionAttribute.fromApi).toSet,
      start = offsetDateTimeSupplier.get,
      end = None
    )
  }
}