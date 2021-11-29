package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import akka.actor.typed.ActorRef
import akka.http.scaladsl.server.directives.FileInfo
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.partymanagement.model.TokenText
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._

import java.util.UUID

/* Command */
sealed trait Command
sealed trait PartyCommand             extends Command
sealed trait PartyRelationshipCommand extends Command
sealed trait TokenCommand             extends Command

case object Idle extends Command

/* Party Command */
final case class AddParty(entity: Party, replyTo: ActorRef[StatusReply[Party]])                 extends PartyCommand
final case class DeleteParty(entity: Party, replyTo: ActorRef[StatusReply[Unit]])               extends PartyCommand
final case class GetParty(partyId: UUID, replyTo: ActorRef[Option[Party]])                      extends PartyCommand
final case class GetPartyAttributes(partyId: UUID, replyTo: ActorRef[StatusReply[Seq[String]]]) extends PartyCommand
final case class AddAttributes(organizationId: UUID, attributes: Seq[String], replyTo: ActorRef[StatusReply[Party]])
    extends PartyCommand
final case class GetOrganizationByExternalId(externalId: String, replyTo: ActorRef[Option[InstitutionParty]])
    extends PartyCommand

/* PartyRelationship Command */
final case class AddPartyRelationship(
  partyRelationship: PersistedPartyRelationship,
  replyTo: ActorRef[StatusReply[Unit]]
) extends PartyRelationshipCommand

final case class ConfirmPartyRelationship(
  relationshipId: UUID,
  filePath: String,
  fileInfo: FileInfo,
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

final case class GetPartyRelationshipsByFrom(from: UUID, replyTo: ActorRef[List[PersistedPartyRelationship]])
    extends PartyRelationshipCommand

final case class GetPartyRelationshipsByTo(to: UUID, replyTo: ActorRef[List[PersistedPartyRelationship]])
    extends PartyRelationshipCommand

final case class GetPartyRelationshipByAttributes(
  from: UUID,
  to: UUID,
  role: PersistedPartyRole,
  product: String,
  productRole: String,
  replyTo: ActorRef[Option[PersistedPartyRelationship]]
) extends PartyRelationshipCommand

/* Token Command */
final case class AddToken(token: Token, replyTo: ActorRef[StatusReply[TokenText]])        extends TokenCommand
final case class DeleteToken(token: Token, replyTo: ActorRef[StatusReply[Unit]])          extends TokenCommand
final case class VerifyToken(token: Token, replyTo: ActorRef[StatusReply[Option[Token]]]) extends TokenCommand

//final case class InvalidateToken(token: Token, replyTo: ActorRef[StatusReply[Token]]) extends TokenCommand

//final case class ConsumeToken(token: Token, replyTo: ActorRef[StatusReply[Token]]) extends TokenCommand
