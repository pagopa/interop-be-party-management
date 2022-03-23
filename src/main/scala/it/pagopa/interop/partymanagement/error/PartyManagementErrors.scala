package it.pagopa.interop.partymanagement.error

import it.pagopa.interop.commons.utils.errors.ComponentError

import java.util.UUID

object PartyManagementErrors {

  final case object CreateOrganizationBadRequest extends ComponentError("0001", "Bad request for organization creation")
  final case object CreateOrganizationConflict   extends ComponentError("0002", "Conflict while creating organization")
  final case class OrganizationAlreadyExists(externalId: String)
      extends ComponentError("0003", s"Organization with external ID $externalId already exists")
  final case class CreateOrganizationError(message: String)
      extends ComponentError("0004", s"Organization creation failed: $message")

  final case object AddAttributesBadRequest extends ComponentError("0005", "Bad request for adding attributes")
  final case object AddAttributesError      extends ComponentError("0006", "Error while adding attributes error")

  final case object CreatePersonBadRequest extends ComponentError("0007", "Bad request for person creation")
  final case object CreatePersonConflict   extends ComponentError("0008", "Conflict while creating person")
  final case object CreatePersonError      extends ComponentError("0009", "Error while creating person creation")

  final case object CreateRelationshipConflict extends ComponentError("0010", "Conflict while creating relationship")
  final case class RelationshipAlreadyExists(relationshipId: UUID)
      extends ComponentError("0011", s"Relationship ${relationshipId} already exists")
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

  final case class GetOrganizationNotFound(id: String) extends ComponentError("0024", s"Organization $id not found")
  final case object GetOrganizationError extends ComponentError("0025", "Error while getting organization")

  final case class GetRelationshipNotFound(id: String) extends ComponentError("0026", s"Relationship $id not found")
  final case object GetRelationshipError extends ComponentError("0027", "Error while getting relationship")

  final case class GetPersonNotFound(id: String) extends ComponentError("0028", s"Person $id not found")
  final case object GetPersonError               extends ComponentError("0029", "Error while getting person")

  final case object GetBulkOrganizationsError
      extends ComponentError("0030", "Error while getting organizations in bulk")

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

  final case class OrganizationNotFound(externalId: String)
      extends ComponentError("0037", s"Organization with external ID $externalId not found")

  final case object OrganizationBadRequest
      extends ComponentError("0038", "Bad request for getting organization by external id")

  final case class TokenExpired(tokenId: String) extends ComponentError("0039", s"Token $tokenId has expired")

  final case class TokenAlreadyConsumed(tokenId: String)
      extends ComponentError("0040", s"Token $tokenId has already consumed")

  final case class TokenVerificationFatalError(tokenId: String, error: String)
      extends ComponentError("0041", s"Something went wrong trying to verify token $tokenId: $error")

}
