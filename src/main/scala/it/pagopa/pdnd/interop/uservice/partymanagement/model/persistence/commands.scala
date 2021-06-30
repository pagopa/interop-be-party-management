package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{Party, PartyRelationShipId, PartyRole, Token}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{AttributeRecord, RelationShip, TokenSeed, TokenText}

import java.util.UUID

/* Command */
sealed trait Command
sealed trait PartyCommand             extends Command
sealed trait PartyRelationShipCommand extends Command
sealed trait TokenCommand             extends Command

case object Idle extends Command

/* Party Command */
final case class AddParty(entity: Party, replyTo: ActorRef[StatusReply[Party]])    extends PartyCommand
final case class DeleteParty(entity: Party, replyTo: ActorRef[StatusReply[State]]) extends PartyCommand
final case class GetParty(id: String, replyTo: ActorRef[Option[Party]])            extends PartyCommand
final case class AddAttributes(
  organizationId: String,
  attributeRecords: Seq[AttributeRecord],
  replyTo: ActorRef[StatusReply[Party]]
) extends PartyCommand

/* PartyRelationShip Command */
final case class AddPartyRelationShip(from: UUID, to: UUID, partyRole: PartyRole, replyTo: ActorRef[StatusReply[State]])
    extends PartyRelationShipCommand

final case class DeletePartyRelationShip(relationShipId: PartyRelationShipId, replyTo: ActorRef[StatusReply[State]])
    extends PartyRelationShipCommand

final case class GetPartyRelationShips(from: UUID, replyTo: ActorRef[StatusReply[List[RelationShip]]])
    extends PartyRelationShipCommand

final case class GetPartyRelationShip(from: UUID, to: UUID, replyTo: ActorRef[StatusReply[Option[RelationShip]]])
    extends PartyRelationShipCommand

/* Party Command */
final case class AddToken(token: TokenSeed, replyTo: ActorRef[StatusReply[TokenText]]) extends TokenCommand

final case class VerifyToken(token: Token, replyTo: ActorRef[StatusReply[Option[Token]]]) extends TokenCommand

final case class InvalidateToken(token: Token, replyTo: ActorRef[StatusReply[State]]) extends TokenCommand

final case class ConsumeToken(token: Token, replyTo: ActorRef[StatusReply[State]]) extends TokenCommand
