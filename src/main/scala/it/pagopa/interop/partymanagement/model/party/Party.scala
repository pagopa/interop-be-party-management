package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.commons.utils.service.UUIDSupplier
import it.pagopa.interop.partymanagement.common.system.ApiParty
import it.pagopa.interop.partymanagement.error.PartyManagementErrors.InvalidParty
import it.pagopa.interop.partymanagement.model.{Attribute, Institution, InstitutionSeed, Person, PersonSeed}
import it.pagopa.interop.partymanagement.service.OffsetDateTimeSupplier

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Future

sealed trait Party {
  def id: UUID
  def start: OffsetDateTime
  def end: Option[OffsetDateTime]

  def addAttributes(attributes: Set[Attribute]): Either[Throwable, Party] = this match {
    case _: PersonParty                     => Left(new RuntimeException("Attributes do not exist for person party"))
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
      case personParty: PersonParty           =>
        Right(Person(id = personParty.id))
      case institutionParty: InstitutionParty =>
        Left(
          Institution(
            id = institutionParty.id,
            institutionId = institutionParty.externalId,
            description = institutionParty.description,
            digitalAddress = institutionParty.digitalAddress,
            taxCode = institutionParty.taxCode,
            address = institutionParty.address,
            zipCode = institutionParty.zipCode,
            origin = institutionParty.origin,
            institutionType = institutionParty.institutionType,
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
  origin: String,
  institutionType: String,
  start: OffsetDateTime,
  end: Option[OffsetDateTime],
  attributes: Set[InstitutionAttribute]
) extends Party

object InstitutionParty {
  def fromApi(
    institution: InstitutionSeed,
    uuidSupplier: UUIDSupplier,
    offsetDateTimeSupplier: OffsetDateTimeSupplier
  ): InstitutionParty = {
    InstitutionParty(
      id = uuidSupplier.get,
      externalId = institution.institutionId,
      description = institution.description,
      digitalAddress = institution.digitalAddress,
      taxCode = institution.taxCode,
      address = institution.address,
      zipCode = institution.zipCode,
      origin = institution.origin,
      institutionType = institution.institutionType,
      attributes = institution.attributes.map(InstitutionAttribute.fromApi).toSet,
      start = offsetDateTimeSupplier.get,
      end = None
    )
  }

  def extractInstitutionParty(party: Party): Future[InstitutionParty] = {
    party match {
      case p: InstitutionParty => Future.successful(p)
      case p                   => Future.failed(InvalidParty(InstitutionParty.toString, p.toString))
    }
  }
}
