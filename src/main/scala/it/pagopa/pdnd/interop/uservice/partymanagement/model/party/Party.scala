package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.ApiParty
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Organization, OrganizationSeed, Person, PersonSeed}
import it.pagopa.pdnd.interop.uservice.partymanagement.service.UUIDSupplier

import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.Equals",
    "org.wartremover.warts.ToString"
  )
)
sealed trait Party {
  def id: UUID
  def externalId: String //TODO describe also the type CF|askjasjdas
  def start: OffsetDateTime
  def end: Option[OffsetDateTime]

  def addAttributes(attributes: Set[String]): Either[Throwable, Party] = this match {
    case _: PersonParty => Left(new RuntimeException("Attributes do not exist for person party"))
    case institutionParty: InstitutionParty =>
      val updated: Set[String] = institutionParty.attributes ++ attributes
      Right(institutionParty.copy(attributes = updated))

  }

}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.Equals",
    "org.wartremover.warts.ToString"
  )
)
object Party {

//  def addAttributes(party: Party, attributes: Set[String]): Either[Throwable, Party] = {
//    val zero: Either[Throwable, Party] = Right(party)
//    attributes.foldLeft(zero)((current, attrs) => current.flatMap(p => p.addAttributes(attrs)))
//  }

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
            managerName = institutionParty.managerName,
            managerSurname = institutionParty.managerSurname,
            digitalAddress = institutionParty.digitalAddress,
            partyId = institutionParty.id.toString,
            attributes = institutionParty.attributes.toSeq
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
  managerName: String,
  managerSurname: String,
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
      managerName = organization.managerName,
      managerSurname = organization.managerSurname,
      attributes = organization.attributes.toSet,
      start = OffsetDateTime.now(),
      end = None
    )
  }
}
