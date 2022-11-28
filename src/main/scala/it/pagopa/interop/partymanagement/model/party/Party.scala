package it.pagopa.interop.partymanagement.model.party

import it.pagopa.interop.commons.utils.service.UUIDSupplier
import it.pagopa.interop.partymanagement.common.system.ApiParty
import it.pagopa.interop.partymanagement.error.PartyManagementErrors.{
  InvalidParty,
  NoAttributeForPartyPerson,
  UpdateInstitutionNotFound
}
import it.pagopa.interop.partymanagement.model._
import it.pagopa.interop.partymanagement.service.OffsetDateTimeSupplier

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Future

sealed trait Party {
  def id: UUID
  def start: OffsetDateTime
  def end: Option[OffsetDateTime]

  def addAttributes(attributes: Set[Attribute]): Either[Throwable, Party] = this match {
    case _: PersonParty                     => Left(NoAttributeForPartyPerson)
    case institutionParty: InstitutionParty =>
      val updated: Set[InstitutionAttribute] = institutionParty.attributes ++ attributes.map(attribute =>
        InstitutionAttribute(origin = attribute.origin, code = attribute.code, description = attribute.description)
      )
      Right(institutionParty.copy(attributes = updated))
  }

  def addPaymentServiceProvider(): Either[Throwable, Party] = this match {
    case _: PersonParty                     => Left(NoAttributeForPartyPerson)
    case institutionParty: InstitutionParty =>
      val updated: Option[PersistedPaymentServiceProvider] = institutionParty.paymentServiceProvider.map(p =>
        PersistedPaymentServiceProvider(
          abiCode = p.abiCode,
          businessRegisterNumber = p.businessRegisterNumber,
          legalRegisterName = p.legalRegisterName,
          legalRegisterNumber = p.legalRegisterNumber,
          vatNumberGroup = p.vatNumberGroup
        )
      )
      Right(institutionParty.copy(paymentServiceProvider = updated))
  }

  def addDataProtectionOfficer(): Either[Throwable, Party] = this match {
    case _: PersonParty                     => Left(NoAttributeForPartyPerson)
    case institutionParty: InstitutionParty =>
      val updated: Option[PersistedDataProtectionOfficer] = institutionParty.dataProtectionOfficer.map(d =>
        PersistedDataProtectionOfficer(address = d.address, email = d.email, pec = d.email)
      )
      Right(institutionParty.copy(dataProtectionOfficer = updated))
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
            externalId = institutionParty.externalId,
            originId = institutionParty.originId,
            description = institutionParty.description,
            digitalAddress = institutionParty.digitalAddress,
            taxCode = institutionParty.taxCode,
            address = institutionParty.address,
            zipCode = institutionParty.zipCode,
            origin = institutionParty.origin,
            institutionType = institutionParty.institutionType,
            attributes = institutionParty.attributes.map(InstitutionAttribute.toApi).toSeq,
            products = (institutionParty.products map { p => p.product -> PersistedInstitutionProduct.toApi(p) }).toMap,
            paymentServiceProvider =
              institutionParty.paymentServiceProvider.map(p => PersistedPaymentServiceProvider.toAPi(p)),
            dataProtectionOfficer =
              institutionParty.dataProtectionOfficer.map(d => PersistedDataProtectionOfficer.toApi(d)),
            geographicTaxonomies = institutionParty.geographicTaxonomies.map(PersistedGeographicTaxonomy.toApi)
          )
        )
    }

  def extractInstitutionParty(partyId: String, party: Option[Party]) = party match {
    case Some(x: InstitutionParty) => Future.successful(x)
    case Some(x: PersonParty)      => Future.failed(InvalidParty("InstitutionParty", x.toString))
    case None                      => Future.failed(UpdateInstitutionNotFound(partyId))
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
  originId: String,
  description: String,
  digitalAddress: String,
  taxCode: String,
  address: String,
  zipCode: String,
  origin: String,
  institutionType: Option[String],
  start: OffsetDateTime,
  end: Option[OffsetDateTime],
  attributes: Set[InstitutionAttribute],
  products: Set[PersistedInstitutionProduct],
  paymentServiceProvider: Option[PersistedPaymentServiceProvider],
  dataProtectionOfficer: Option[PersistedDataProtectionOfficer],
  geographicTaxonomies: Seq[PersistedGeographicTaxonomy]
) extends Party

object InstitutionParty {
  def fromApi(
    institution: InstitutionSeed,
    uuidSupplier: UUIDSupplier,
    offsetDateTimeSupplier: OffsetDateTimeSupplier
  ): InstitutionParty = {
    InstitutionParty(
      id = uuidSupplier.get,
      externalId = institution.externalId,
      originId = institution.originId,
      description = institution.description,
      digitalAddress = institution.digitalAddress,
      taxCode = institution.taxCode,
      address = institution.address,
      zipCode = institution.zipCode,
      origin = institution.origin,
      institutionType = institution.institutionType,
      attributes = institution.attributes.map(InstitutionAttribute.fromApi).toSet,
      start = offsetDateTimeSupplier.get,
      end = None,
      products = institution.products.fold(Set.empty[PersistedInstitutionProduct])(
        _.values.map(PersistedInstitutionProduct.fromApi).toSet
      ),
      paymentServiceProvider = institution.paymentServiceProvider.map(PersistedPaymentServiceProvider.fromApi),
      dataProtectionOfficer = institution.dataProtectionOfficer.map(PersistedDataProtectionOfficer.fromApi),
      geographicTaxonomies = institution.geographicTaxonomies.fold(Seq.empty[PersistedGeographicTaxonomy])(
        _.map(PersistedGeographicTaxonomy.fromApi)
      )
    )
  }

  def fromInstitution(
    institution: Institution
  )(id: UUID, start: OffsetDateTime, end: Option[OffsetDateTime]): InstitutionParty =
    InstitutionParty(
      id = id,
      externalId = institution.externalId,
      originId = institution.originId,
      description = institution.description,
      digitalAddress = institution.digitalAddress,
      taxCode = institution.taxCode,
      address = institution.address,
      zipCode = institution.zipCode,
      origin = institution.origin,
      institutionType = institution.institutionType,
      start = start,
      end = end,
      attributes = institution.attributes.map(InstitutionAttribute.fromApi).toSet,
      products = institution.products.values.map(PersistedInstitutionProduct.fromApi).toSet,
      paymentServiceProvider = institution.paymentServiceProvider.map(PersistedPaymentServiceProvider.fromApi),
      dataProtectionOfficer = institution.dataProtectionOfficer.map(PersistedDataProtectionOfficer.fromApi),
      geographicTaxonomies = institution.geographicTaxonomies.map(PersistedGeographicTaxonomy.fromApi)
    )

}
