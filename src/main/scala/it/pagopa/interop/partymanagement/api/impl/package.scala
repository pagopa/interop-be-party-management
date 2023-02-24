package it.pagopa.interop.partymanagement.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCode
import it.pagopa.interop.partymanagement.model._
import it.pagopa.interop.commons.utils.SprayCommonFormats.{offsetDateTimeFormat, uuidFormat}
import it.pagopa.interop.commons.utils.errors.ComponentError
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val dataProtectionOfficerFormat: RootJsonFormat[DataProtectionOfficer]   = jsonFormat3(DataProtectionOfficer)
  implicit val paymentServiceProviderFormat: RootJsonFormat[PaymentServiceProvider] = jsonFormat5(
    PaymentServiceProvider
  )
  implicit val geographicTaxonomyFormat: RootJsonFormat[GeographicTaxonomy]         = jsonFormat2(GeographicTaxonomy)
  implicit val institutionUpdateFormat: RootJsonFormat[InstitutionUpdate]           = jsonFormat15(InstitutionUpdate)
  implicit val billingFormat: RootJsonFormat[Billing]                               = jsonFormat3(Billing)
  implicit val onboardingContractFormat: RootJsonFormat[OnboardingContract]         = jsonFormat2(OnboardingContract)
  implicit val institutionProductFormat: RootJsonFormat[InstitutionProduct]         = jsonFormat3(InstitutionProduct)
  implicit val attributeFormat: RootJsonFormat[Attribute]                           = jsonFormat3(Attribute)
  implicit val personSeedFormat: RootJsonFormat[PersonSeed]                         = jsonFormat1(PersonSeed)
  implicit val personFormat: RootJsonFormat[Person]                                 = jsonFormat1(Person)

  implicit val institutionSeedFormat: RootJsonFormat[InstitutionSeed]                 = jsonFormat20(InstitutionSeed)
  implicit val institutionFormat: RootJsonFormat[Institution]                         = jsonFormat21(Institution)
  implicit val institutionIdFormat: RootJsonFormat[InstitutionId]                     = jsonFormat4(InstitutionId)
  implicit val institutionsFormat: RootJsonFormat[Institutions]                       = jsonFormat1(Institutions)
  implicit val relationshipProductFormat: RootJsonFormat[RelationshipProduct]         = jsonFormat3(RelationshipProduct)
  implicit val relationshipFormat: RootJsonFormat[Relationship]                       = jsonFormat15(Relationship)
  implicit val relationshipProductSeedFormat: RootJsonFormat[RelationshipProductSeed] = jsonFormat2(
    RelationshipProductSeed
  )
  implicit val relationshipSeedFormat: RootJsonFormat[RelationshipSeed]               = jsonFormat8(RelationshipSeed)
  implicit val relationshipsFormat: RootJsonFormat[Relationships]                     = jsonFormat1(Relationships)
  implicit val relationshipsSeedFormat: RootJsonFormat[RelationshipsSeed]             = jsonFormat1(RelationshipsSeed)
  implicit val problemErrorFormat: RootJsonFormat[ProblemError]                       = jsonFormat2(ProblemError)
  implicit val problemFormat: RootJsonFormat[Problem]                                 = jsonFormat5(Problem)
  implicit val onboardingContractInfoFormat: RootJsonFormat[OnboardingContractInfo]   = jsonFormat2(
    OnboardingContractInfo
  )
  implicit val tokenSeedFormat: RootJsonFormat[TokenSeed]                             = jsonFormat4(TokenSeed)
  implicit val tokenTextFormat: RootJsonFormat[TokenText]                             = jsonFormat1(TokenText)
  implicit val relationshipBindingFormat: RootJsonFormat[RelationshipBinding]         = jsonFormat3(RelationshipBinding)
  implicit val tokenInfoFormat: RootJsonFormat[TokenInfo]                             = jsonFormat3(TokenInfo)
  implicit val bulkPartiesSeedFormat: RootJsonFormat[BulkPartiesSeed]                 = jsonFormat1(BulkPartiesSeed)
  implicit val bulkInstitutionsFormat: RootJsonFormat[BulkInstitutions]               = jsonFormat2(BulkInstitutions)
  implicit val digestSeedFormat: RootJsonFormat[DigestSeed]                           = jsonFormat1(DigestSeed)

  implicit val newDesignUserInstitutionProductFormat: RootJsonFormat[NewDesignUserInstitutionProduct] = jsonFormat8(
    NewDesignUserInstitutionProduct
  )
  implicit val newDesignUserInstitutionFormat: RootJsonFormat[NewDesignUserInstitution]               = jsonFormat3(
    NewDesignUserInstitution
  )
  implicit val newDesignUserFormat: RootJsonFormat[NewDesignUser] = jsonFormat4(NewDesignUser)

  final val serviceErrorCodePrefix: String = "001"
  final val defaultProblemType: String     = "about:blank"

  def problemOf(httpError: StatusCode, error: ComponentError, defaultMessage: String = "Unknown error"): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      errors = Seq(
        ProblemError(
          code = s"$serviceErrorCodePrefix-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultMessage)
        )
      )
    )

  final val ipaOrigin: String = "IPA"
}
