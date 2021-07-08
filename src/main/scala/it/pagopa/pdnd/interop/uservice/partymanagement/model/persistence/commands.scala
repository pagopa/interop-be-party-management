package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{AttributeRecord, TokenSeed, TokenText}

import java.util.UUID

/* Command */
sealed trait Command
sealed trait PartyCommand             extends Command
sealed trait PartyRelationShipCommand extends Command
sealed trait TokenCommand             extends Command

case object Idle extends Command

/* Party Command */
final case class AddParty(entity: Party, shardId: String, replyTo: ActorRef[StatusReply[Party]]) extends PartyCommand
final case class DeleteParty(entity: Party, replyTo: ActorRef[StatusReply[Unit]])                extends PartyCommand
final case class GetParty(partyId: UUID, replyTo: ActorRef[Option[Party]])                       extends PartyCommand
final case class GetPartyByExternalId(externalId: String, shardId: String, replyTo: ActorRef[Option[Party]])
    extends PartyCommand
final case class AddAttributes(
  organizationId: String,
  attributeRecords: Seq[AttributeRecord],
  replyTo: ActorRef[StatusReply[Party]]
) extends PartyCommand

/* PartyRelationShip Command */
final case class AddPartyRelationShip(from: UUID, to: UUID, partyRole: PartyRole, replyTo: ActorRef[StatusReply[Unit]])
    extends PartyRelationShipCommand

final case class ConfirmPartyRelationShip(relationShipId: PartyRelationShipId, replyTo: ActorRef[StatusReply[Unit]])
    extends PartyRelationShipCommand

final case class DeletePartyRelationShip(relationShipId: PartyRelationShipId, replyTo: ActorRef[StatusReply[Unit]])
    extends PartyRelationShipCommand

final case class GetPartyRelationShips(from: UUID, replyTo: ActorRef[List[PartyRelationShip]])
    extends PartyRelationShipCommand

/* Party Command */
final case class AddToken(
  token: TokenSeed,
  partyRelationShipIds: Seq[PartyRelationShipId],
  replyTo: ActorRef[StatusReply[TokenText]]
)                                                                                         extends TokenCommand
final case class DeleteToken(token: Token, replyTo: ActorRef[StatusReply[Unit]])          extends TokenCommand
final case class VerifyToken(token: Token, replyTo: ActorRef[StatusReply[Option[Token]]]) extends TokenCommand

//final case class InvalidateToken(token: Token, replyTo: ActorRef[StatusReply[Token]]) extends TokenCommand

//final case class ConsumeToken(token: Token, replyTo: ActorRef[StatusReply[Token]]) extends TokenCommand
