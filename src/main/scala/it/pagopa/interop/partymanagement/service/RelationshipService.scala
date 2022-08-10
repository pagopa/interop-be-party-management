package it.pagopa.interop.partymanagement.service

import akka.util.Timeout
import it.pagopa.interop.partymanagement.model.Relationship

import scala.concurrent.Future

trait RelationshipService {
  def getRelationshipById(relationshipId: String)(implicit timeout: Timeout): Future[Option[Relationship]]
}
