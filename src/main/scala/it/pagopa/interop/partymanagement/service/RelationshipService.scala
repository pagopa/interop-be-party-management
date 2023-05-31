package it.pagopa.interop.partymanagement.service

import akka.util.Timeout
import it.pagopa.interop.partymanagement.model.{PartyRole, Relationship, RelationshipState}

import java.util.UUID
import scala.concurrent.Future

trait RelationshipService {
  def retrieveRelationshipsByTo(
    id: UUID,
    roles: List[PartyRole],
    states: List[RelationshipState],
    product: List[String],
    productRoles: List[String]
  )(implicit timeout: Timeout): Future[List[Relationship]]
  def retrieveRelationshipsByFrom(
    id: UUID,
    roles: List[PartyRole],
    states: List[RelationshipState],
    product: List[String],
    productRoles: List[String]
  )(implicit timeout: Timeout): Future[List[Relationship]]
  def relationshipsFromParams(
    from: Option[UUID],
    to: Option[UUID],
    roles: List[PartyRole],
    states: List[RelationshipState],
    product: List[String],
    productRoles: List[String]
  )(implicit timeout: Timeout): Future[List[Relationship]]
  def getRelationshipById(relationshipId: UUID)(implicit timeout: Timeout): Future[Option[Relationship]]
  def getRelationshipsByUserIds(userId: List[UUID])(implicit timeout: Timeout): Future[Seq[Relationship]]
  def getRelationships()(implicit timeout: Timeout): Future[Seq[Relationship]]
}
