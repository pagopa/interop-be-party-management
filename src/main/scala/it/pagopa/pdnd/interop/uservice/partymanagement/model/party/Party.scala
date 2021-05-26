package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.ApiParty
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Organization, OrganizationSeed, Person, PersonSeed}
import it.pagopa.pdnd.interop.uservice.partymanagement.service.UUIDSupplier

import java.time.OffsetDateTime
import java.util.UUID

sealed trait Party {
  def id: UUID // TODO probably not necessary
  def externalId: String
  def start: OffsetDateTime
  def end: Option[OffsetDateTime]

  def addAttributes(attributes: Attributes): Either[Throwable, Party] = this match {
    case _: PersonParty => Left(new RuntimeException("Attributes do not exist for person party"))
    case institutionParty: InstitutionParty => {
      val updated: Set[Attributes] = institutionParty.attributes.map {
        case xs if xs.kind == attributes.kind => xs.copy(values = xs.values.union(attributes.values))
        case xs                               => xs
      }
      Right(institutionParty.copy(attributes = updated))
    }
  }

}

object Party {

  def addAttributes(party: Party, attributes: Set[Attributes]): Either[Throwable, Party] = {
    val zero: Either[Throwable, Party] = Right(party)
    attributes.foldLeft(zero)((current, attrs) => current.flatMap(p => p.addAttributes(attrs)))
  }

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
            partyId = institutionParty.id.toString,
            attributes = institutionParty.attributes.map(_.toApi).toSeq
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
  def fromApi(person: PersonSeed, uuidSupplier: UUIDSupplier): PersonParty = PersonParty(
    id = uuidSupplier.get,
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
  attributes: Set[Attributes],
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
      manager = organization.manager,
      attributes = organization.attributes.map(Attributes.fromApi).toSet,
      start = OffsetDateTime.now(),
      end = None
    )
  }
}
