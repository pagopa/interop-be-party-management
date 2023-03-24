package it.pagopa.interop.partymanagement.error

import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.partymanagement.model.CollectionSearchMode

import java.util.UUID

object PartyManagementErrors {

  final case object CreateInstitutionBadRequest extends ComponentError("0001", "Bad request for institution creation")
  final case object CreateInstitutionConflict   extends ComponentError("0002", "Conflict while creating institution")
  final case class InstitutionAlreadyExists(externalId: String)
      extends ComponentError("0003", s"Institution with external ID $externalId already exists")
  final case class CreateInstitutionError(message: String)
      extends ComponentError("0004", s"Institution creation failed: $message")

  final case object AddAttributesBadRequest extends ComponentError("0005", "Bad request for adding attributes")
  final case object AddAttributesError      extends ComponentError("0006", "Error while adding attributes error")

  final case object CreatePersonBadRequest extends ComponentError("0007", "Bad request for person creation")
  final case object CreatePersonConflict   extends ComponentError("0008", "Conflict while creating person")
  final case object CreatePersonError      extends ComponentError("0009", "Error while creating person creation")

  final case object CreateRelationshipConflict extends ComponentError("0010", "Conflict while creating relationship")
  final case class RelationshipAlreadyExists(relationshipId: UUID)
      extends ComponentError("0011", s"Relationship $relationshipId already exists")
  final case class CreateRelationshipError(message: String)
      extends ComponentError("0012", "Error while creating relationship")

  final case object GetRelationshipsError extends ComponentError("0013", "Error while getting relationships")

  final case class TokenNotFound(tokenId: String) extends ComponentError("0014", s"Token $tokenId not found")
  final case class GetTokenFatalError(tokenId: String, error: String)
      extends ComponentError("0015", s"Something went wrong trying to get token $tokenId: $error")

  final case class ConsumeTokenBadRequest(errors: String)
      extends ComponentError("0016", s"Error while consuming token with errors: $errors")
  final case class ConsumeTokenError(message: String)
      extends ComponentError("0017", s"Error while consuming token: $message")

  final case class InvalidateTokenBadRequest(errors: String)
      extends ComponentError("0018", s"Error while invalidating token with errors: $errors")
  final case class InvalidateTokenError(message: String)
      extends ComponentError("0019", s"Error while invalidating token: $message")

  final case class CreateTokenBadRequest(errors: String)
      extends ComponentError("0020", s"Error while creating token with errors: $errors")
  final case class CreateTokenError(message: String)
      extends ComponentError("0021", s"Error while creating token: $message")

  final case object GetPartyAttributesError extends ComponentError("0022", "Error while getting party attributes")
  final case object PartyAttributesNotFound
      extends ComponentError("0023", "Error while getting party attributes - party not found")

  final case class GetInstitutionNotFound(id: String) extends ComponentError("0024", s"Institution $id not found")
  final case object GetInstitutionError               extends ComponentError("0025", "Error while getting institution")

  final case class GetRelationshipNotFound(id: String) extends ComponentError("0026", s"Relationship $id not found")
  final case object GetRelationshipError extends ComponentError("0027", "Error while getting relationship")

  final case class GetPersonNotFound(id: String) extends ComponentError("0028", s"Person $id not found")
  final case object GetPersonError               extends ComponentError("0029", "Error while getting person")

  final case object GetBulkInstitutionsError extends ComponentError("0030", "Error while getting institutions in bulk")

  final case class ActivateRelationshipError(id: String)
      extends ComponentError("0031", s"Error while activating relationship with id $id")

  final case object SuspendingRelationshipError extends ComponentError("0032", "Error while suspending relationship")

  final case class DeletingRelationshipError(id: String)
      extends ComponentError("0033", s"Error while deleting relationship $id")
  final case class DeletingRelationshipNotFound(id: String)
      extends ComponentError("0034", s"Error while deleting relationship $id - not found")
  final case class DeletingRelationshipBadRequest(id: String)
      extends ComponentError("0035", s"Error while deleting relationship $id - bad request")

  final case class ManagerNotSupplied(tokenId: String)
      extends ComponentError("0036", s"Token $tokenId can't be generated because no manager party has been supplied")

  final case class InstitutionNotFound(externalId: String)
      extends ComponentError("0037", s"Institution with external ID $externalId not found")

  final case object InstitutionBadRequest
      extends ComponentError("0038", "Bad request for getting institution by external id")

  final case class TokenExpired(tokenId: String) extends ComponentError("0039", s"Token $tokenId has expired")

  final case class TokenAlreadyConsumed(tokenId: String)
      extends ComponentError("0040", s"Token $tokenId has already consumed")

  final case class TokenVerificationFatalError(tokenId: String, error: String)
      extends ComponentError("0041", s"Something went wrong trying to verify token $tokenId: $error")

  final case class InvalidParty(expectedType: String, obtained: String)
      extends ComponentError("0042", s"Something went wrong reading party as $expectedType: $obtained")

  final case class UpdateInstitutionNotFound(id: String)
      extends ComponentError("0043", s"Cannot find institution having id $id")

  final case class UpdateInstitutionBadRequest(institutionId: String, cause: String)
      extends ComponentError("0044", s"Something went wrong updating institution as $institutionId: $cause")

  final case object MissingQueryParam
      extends ComponentError("0045", s"At least one query parameter between [from, to] must be passed")

  final case class PartyNotFound(partyId: String) extends ComponentError("0046", s"Party $partyId not found")

  final case object NoValidManagerFound extends ComponentError("0047", s"Operator without active manager")

  final case object RelationshipNotFound extends ComponentError("0048", s"Relationship not found")

  final case object NoAttributeForPartyPerson
      extends ComponentError("0049", s"Attributes do not exist for person party")

  final case class InstitutionsWithProductNotFound(productId: String)
      extends ComponentError("0050", s"Institutions with product ID $productId not found")

  final case class EnableRelationshipError(id: String)
      extends ComponentError("0051", s"Error while enabling relationship with id $id")

  final case class CollectionSearchModeNotValid(searchMode: Option[String])
      extends ComponentError("0052", s"Invalid CollectionSearchMode $searchMode")

  final case class FindByGeoTaxonomiesError(geoTaxonomies: String, searchMode: Option[String])
      extends ComponentError(
        "0053",
        s"Error while searching institutions related to $geoTaxonomies and searchMode $searchMode"
      )

  final case class FindByGeoTaxonomiesInvalid(
    desc: String,
    geoTaxonomies: Set[String],
    searchMode: CollectionSearchMode
  ) extends ComponentError(
        "0054",
        s"Invalid search configuration while searching institutions related to $geoTaxonomies and searchMode $searchMode: $desc"
      )

  final case class BillingRelationshipError(id: String)
      extends ComponentError("0055", s"Error while updating billing data for relationship $id")

  final case class BillingRelationshipNotFound(id: String)
      extends ComponentError("0056", s"Error while updating billing data for relationship $id - not found")

  final case class BillingRelationshipBadRequest(id: String)
      extends ComponentError("0057", s"Error while updating billing data for relationship $id - bad request")

  final case class DeleteTokenBadRequest(errors: String)
      extends ComponentError("0058", s"Error while deleting token with errors: $errors")

  final case class DeleteTokenError(message: String)
      extends ComponentError("0059", s"Error while deleting token: $message")

  final case class FindNewDesignUserError(desc: String)
      extends ComponentError("0060", s"Error while returning users mapping them into new design: $desc")

  final case class FindNewDesignInstitutionError(desc: String)
      extends ComponentError("0061", s"Error while returning institutions mapping them into new design: $desc")

  final case class FindNewDesignTokenError(desc: String)
      extends ComponentError("0062", s"Error while returning tokens mapping them into new design: $desc")
}
