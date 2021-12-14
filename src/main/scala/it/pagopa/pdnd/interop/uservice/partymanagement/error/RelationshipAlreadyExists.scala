package it.pagopa.pdnd.interop.uservice.partymanagement.error

import java.util.UUID

final case class RelationshipAlreadyExists(relationshipId: UUID)
    extends Throwable(s"Relationship ${relationshipId} already exists")
