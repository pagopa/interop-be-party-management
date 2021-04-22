package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.ApiParty
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{RelationShip, TokenText}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{Party, PartyRelationShipId, PartyRole, Token}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.PartyPersistentBehavior.State

import java.util.UUID

/* Command */
sealed trait Command                  extends CborSerializable
sealed trait PartyCommand             extends Command
sealed trait PartyRelationShipCommand extends Command
sealed trait TokenCommand             extends Command

/* Party Command */
final case class AddParty(entity: Party, replyTo: ActorRef[StatusReply[ApiParty]])      extends PartyCommand
final case class DeleteParty(entity: Party, replyTo: ActorRef[StatusReply[State]])      extends PartyCommand
final case class GetParty(id: String, replyTo: ActorRef[StatusReply[Option[ApiParty]]]) extends PartyCommand

/* PartyRelationShip Command */
final case class AddPartyRelationShip(from: UUID, to: UUID, partyRole: PartyRole, replyTo: ActorRef[StatusReply[State]])
    extends PartyRelationShipCommand

final case class DeletePartyRelationShip(relationShipId: PartyRelationShipId, replyTo: ActorRef[StatusReply[State]])
    extends PartyRelationShipCommand

final case class GetPartyRelationShip(taxCode: UUID, replyTo: ActorRef[StatusReply[List[RelationShip]]])
    extends PartyRelationShipCommand

/* Party Command */
final case class AddToken(token: Token, replyTo: ActorRef[StatusReply[TokenText]]) extends TokenCommand

final case class InvalidateToken(token: Token, replyTo: ActorRef[StatusReply[State]]) extends TokenCommand

final case class ConsumeToken(token: Token, replyTo: ActorRef[StatusReply[State]]) extends TokenCommand
