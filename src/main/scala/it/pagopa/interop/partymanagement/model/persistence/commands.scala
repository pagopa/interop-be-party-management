package it.pagopa.interop.partymanagement.model.persistence

import akka.actor.typed.ActorRef
import akka.http.scaladsl.server.directives.FileInfo
import akka.pattern.StatusReply
import it.pagopa.interop.partymanagement.model.party.PersistedPartyRelationship
import it.pagopa.interop.partymanagement.model.party._
import it.pagopa.interop.partymanagement.model.{Attribute, PartyRole, RelationshipState, TokenText}

import java.util.UUID

/* Command */
sealed trait Command
sealed trait PartyCommand             extends Command
sealed trait PartyRelationshipCommand extends Command
sealed trait TokenCommand             extends Command

case object Idle extends Command

/* Party Command */
final case class AddParty(entity: Party, replyTo: ActorRef[StatusReply[Party]])    extends PartyCommand
final case class UpdateParty(entity: Party, replyTo: ActorRef[StatusReply[Party]]) extends PartyCommand
final case class DeleteParty(entity: Party, replyTo: ActorRef[StatusReply[Unit]])  extends PartyCommand
final case class GetParty(partyId: UUID, replyTo: ActorRef[Option[Party]])         extends PartyCommand
final case class GetPartyAttributes(partyId: UUID, replyTo: ActorRef[StatusReply[Seq[InstitutionAttribute]]])
    extends PartyCommand
final case class AddAttributes(id: UUID, attributes: Seq[Attribute], replyTo: ActorRef[StatusReply[Party]])
    extends PartyCommand
final case class GetInstitutionByExternalId(externalId: String, replyTo: ActorRef[Option[InstitutionParty]])
    extends PartyCommand

/* PartyRelationship Command */
final case class AddPartyRelationship(
  partyRelationship: PersistedPartyRelationship,
  replyTo: ActorRef[StatusReply[PersistedPartyRelationship]]
) extends PartyRelationshipCommand

final case class ConfirmPartyRelationship(
  relationshipId: UUID,
  filePath: String,
  fileInfo: FileInfo,
  tokenId: UUID,
  replyTo: ActorRef[StatusReply[Unit]]
) extends PartyRelationshipCommand

final case class RejectPartyRelationship(relationshipId: UUID, replyTo: ActorRef[StatusReply[Unit]])
    extends PartyRelationshipCommand

final case class SuspendPartyRelationship(relationshipId: UUID, replyTo: ActorRef[StatusReply[Unit]])
    extends PartyRelationshipCommand

final case class ActivatePartyRelationship(relationshipId: UUID, replyTo: ActorRef[StatusReply[Unit]])
    extends PartyRelationshipCommand

final case class DeletePartyRelationship(relationshipId: UUID, replyTo: ActorRef[StatusReply[Unit]])
    extends PartyRelationshipCommand

final case class GetPartyRelationshipById(relationshipId: UUID, replyTo: ActorRef[Option[PersistedPartyRelationship]])
    extends PartyRelationshipCommand

final case class GetPartyRelationshipsByFrom(
  from: UUID,
  roles: List[PartyRole],
  states: List[RelationshipState],
  products: List[String],
  productRoles: List[String],
  replyTo: ActorRef[List[PersistedPartyRelationship]]
) extends PartyRelationshipCommand

final case class GetPartyRelationshipsByTo(
  to: UUID,
  roles: List[PartyRole],
  states: List[RelationshipState],
  products: List[String],
  productRoles: List[String],
  replyTo: ActorRef[List[PersistedPartyRelationship]]
) extends PartyRelationshipCommand

final case class GetPartyRelationshipByAttributes(
  from: UUID,
  to: UUID,
  role: PersistedPartyRole,
  product: String,
  productRole: String,
  replyTo: ActorRef[Option[PersistedPartyRelationship]]
) extends PartyRelationshipCommand

/* Token Command */
final case class GetToken(tokenId: UUID, replyTo: ActorRef[Option[Token]])         extends TokenCommand
final case class AddToken(token: Token, replyTo: ActorRef[StatusReply[TokenText]]) extends TokenCommand
