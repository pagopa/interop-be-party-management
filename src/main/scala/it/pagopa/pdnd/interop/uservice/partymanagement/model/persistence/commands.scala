package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{TokenSeed, TokenText}

import java.util.UUID

/* Command */
sealed trait Command
sealed trait PartyCommand             extends Command
sealed trait PartyRelationshipCommand extends Command
sealed trait TokenCommand             extends Command

case object Idle extends Command

/* Party Command */
final case class AddParty(entity: Party, shardId: String, replyTo: ActorRef[StatusReply[Party]]) extends PartyCommand
final case class DeleteParty(entity: Party, replyTo: ActorRef[StatusReply[Unit]])                extends PartyCommand
final case class GetParty(partyId: UUID, replyTo: ActorRef[Option[Party]])                       extends PartyCommand
final case class GetPartyAttributes(partyId: UUID, replyTo: ActorRef[StatusReply[Seq[String]]])  extends PartyCommand
final case class GetPartyByExternalId(externalId: String, shardId: String, replyTo: ActorRef[Option[Party]])
    extends PartyCommand
final case class AddAttributes(organizationId: String, attributes: Seq[String], replyTo: ActorRef[StatusReply[Party]])
    extends PartyCommand

/* PartyRelationship Command */
final case class AddPartyRelationship(partyRelationship: PartyRelationship, replyTo: ActorRef[StatusReply[Unit]])
    extends PartyRelationshipCommand

final case class ConfirmPartyRelationship(relationshipId: PartyRelationshipId, replyTo: ActorRef[StatusReply[Unit]])
    extends PartyRelationshipCommand

final case class DeletePartyRelationship(relationshipId: PartyRelationshipId, replyTo: ActorRef[StatusReply[Unit]])
    extends PartyRelationshipCommand

final case class GetPartyRelationshipsByFrom(from: UUID, replyTo: ActorRef[List[PartyRelationship]])
    extends PartyRelationshipCommand

final case class GetPartyRelationshipsByTo(to: UUID, replyTo: ActorRef[List[PartyRelationship]])
    extends PartyRelationshipCommand

/* Party Command */
final case class AddToken(
  token: TokenSeed,
  partyRelationshipIds: Seq[PartyRelationshipId],
  replyTo: ActorRef[StatusReply[TokenText]]
)                                                                                         extends TokenCommand
final case class DeleteToken(token: Token, replyTo: ActorRef[StatusReply[Unit]])          extends TokenCommand
final case class VerifyToken(token: Token, replyTo: ActorRef[StatusReply[Option[Token]]]) extends TokenCommand

//final case class InvalidateToken(token: Token, replyTo: ActorRef[StatusReply[Token]]) extends TokenCommand

//final case class ConsumeToken(token: Token, replyTo: ActorRef[StatusReply[Token]]) extends TokenCommand
