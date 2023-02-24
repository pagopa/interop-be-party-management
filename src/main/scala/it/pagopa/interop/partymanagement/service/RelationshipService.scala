package it.pagopa.interop.partymanagement.service

import akka.util.Timeout
import it.pagopa.interop.partymanagement.model.Relationship

import java.util.UUID
import scala.concurrent.Future

trait RelationshipService {
  def getRelationshipById(relationshipId: UUID)(implicit timeout: Timeout): Future[Option[Relationship]]
  def getRelationshipsByUserIds(userId: List[UUID])(implicit timeout: Timeout): Future[Seq[Relationship]]
  def getRelationships()(implicit timeout: Timeout): Future[Seq[Relationship]]
}
