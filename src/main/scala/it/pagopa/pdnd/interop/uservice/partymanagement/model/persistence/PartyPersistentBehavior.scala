package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.ApiParty
import it.pagopa.pdnd.interop.uservice.partymanagement.model.TokenText
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.Party.convertToApi
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import org.slf4j.LoggerFactory

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object PartyPersistentBehavior {

  private val logger = LoggerFactory.getLogger(this.getClass)

  final case class State(
    parties: Map[UUID, Party],
    indexes: Map[String, UUID],
    tokens: Map[RelationShipId, List[Token]],
    relationShips: Map[RelationShipId, RelationShip]
  ) extends CborSerializable {

    def addParty(party: Party): State =
      copy(parties = parties + (party.id -> party), indexes = indexes + (party.externalId -> party.id))

    def deleteParty(party: Party): State = copy(parties = parties - party.id, indexes = indexes - party.externalId)

    def addPartyRelationShip(relationShip: RelationShip): State =
      copy(relationShips = relationShips + (relationShip.id -> relationShip))

    def deletePartyRelationShip(relationShipId: RelationShipId): State =
      copy(relationShips = relationShips - relationShipId)

    def addToken(token: Token): State = {
      tokens.get(token.relationShipId) match {
        case Some(ts: List[Token]) => copy(tokens = tokens + (token.relationShipId -> (token :: ts)))
        case None                  => copy(tokens = tokens + (token.relationShipId -> (token :: Nil)))
      }
    }

    def invalidateToken(relationShipId: RelationShipId): State =
      changeTokenStatus(relationShipId, TokenStatus.Invalid)

    def consumeToken(relationShipId: RelationShipId): State =
      changeTokenStatus(relationShipId, TokenStatus.Consumed)

    private def changeTokenStatus(relationShipId: RelationShipId, status: TokenStatus): State = {
      val modified = tokens(relationShipId).map {
        case token if token.relationShipId == relationShipId => token.copy(status = status)
        case token                                           => token
      }

      if (status == TokenStatus.Consumed) {
        val relationShip = relationShips(relationShipId).copy(status = RelationShipStatus.Active)
        copy(
          relationShips = relationShips + (relationShipId -> relationShip),
          tokens = tokens + (relationShipId               -> modified)
        )
      } else copy(tokens = tokens + (relationShipId -> modified))

    }

  }

  object State {
    val empty: State = State(parties = Map.empty, indexes = Map.empty, relationShips = Map.empty, tokens = Map.empty)
  }

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
  final case class AddPartyRelationShip(
    from: UUID,
    to: UUID,
    partyRole: PartyRole,
    replyTo: ActorRef[StatusReply[State]]
  ) extends PartyRelationShipCommand

  final case class DeletePartyRelationShip(relationShipId: RelationShipId, replyTo: ActorRef[StatusReply[State]])
      extends PartyRelationShipCommand

  final case class GetPartyRelationShip(
    relationShipId: RelationShipId,
    replyTo: ActorRef[StatusReply[Option[RelationShip]]]
  ) extends PartyRelationShipCommand

  /* Party Command */
  final case class AddToken(token: Token, replyTo: ActorRef[StatusReply[TokenText]]) extends TokenCommand

  final case class InvalidateToken(relationShipId: RelationShipId, replyTo: ActorRef[StatusReply[State]])
      extends TokenCommand

  final case class ConsumeToken(relationShipId: RelationShipId, replyTo: ActorRef[StatusReply[State]])
      extends TokenCommand

  /* Event */
  sealed trait Event                  extends CborSerializable
  sealed trait PartyEvent             extends Event
  sealed trait PartyRelationShipEvent extends Event
  sealed trait TokenEvent             extends Event

  /* Party Event */
  final case class PartyAdded(party: Party)   extends PartyEvent
  final case class PartyDeleted(party: Party) extends PartyEvent

  /* PartyRelationShip Event */
  final case class PartyRelationShipAdded(partyRelationShip: RelationShip)  extends PartyRelationShipEvent
  final case class PartyRelationShipDeleted(relationShipId: RelationShipId) extends PartyRelationShipEvent

  /* Token Event */
  final case class TokenAdded(token: Token)                         extends TokenEvent
  final case class TokenInvalidated(relationShipId: RelationShipId) extends TokenEvent
  final case class TokenConsumed(relationShipId: RelationShipId)    extends TokenEvent

  val commandHandler: (State, Command) => Effect[Event, State] = { (state, command) =>
    command match {
      case AddParty(party, replyTo) =>
        logger.info(s"Adding party ${party.externalId}")

        state.indexes
          .get(party.externalId)
          .map { _ =>
            replyTo ! StatusReply.Error(s"Party ${party.externalId} already exists")
            Effect.none[PartyAdded, State]
          }
          .getOrElse {
            Effect
              .persist(PartyAdded(party))
              .thenRun(_ => replyTo ! StatusReply.Success(convertToApi(party)))
          }

      case DeleteParty(party, replyTo) =>
        Effect
          .persist(PartyDeleted(party))
          .thenRun(state => replyTo ! StatusReply.Success(state))

      case GetParty(id, replyTo) =>
        logger.info(s"Getting party $id")

        val party: Option[ApiParty] = for {
          uuid <- state.indexes.get(id)
          _ = logger.info(s"Found $id/${uuid.toString}")
          party <- state.parties.get(uuid)
        } yield convertToApi(party)

        replyTo ! StatusReply.Success(party)

        Effect.none

      case AddPartyRelationShip(from, to, role, replyTo) =>
        val partyRelationShip: RelationShip = RelationShip.create(from, to, role)

        state.relationShips
          .get(partyRelationShip.id)
          .map { _ =>
            replyTo ! StatusReply.Error(s"Relationship ${partyRelationShip.id.stringify} already exists")
            Effect.none[PartyAdded, State]
          }
          .getOrElse {
            Effect
              .persist(PartyRelationShipAdded(partyRelationShip))
              .thenRun(state => replyTo ! StatusReply.Success(state))
          }

      case DeletePartyRelationShip(relationShipId, replyTo) =>
        Effect
          .persist(PartyRelationShipDeleted(relationShipId))
          .thenRun(state => replyTo ! StatusReply.Success(state))

      case GetPartyRelationShip(relationShipId, replyTo) =>
        val partyRelationShip: Option[RelationShip] = state.relationShips.get(relationShipId)

        replyTo ! StatusReply.Success(partyRelationShip)

        Effect.none

      case AddToken(token, replyTo) =>
        val isEligible: Boolean =
          !checkTokens(
            state,
            token.relationShipId,
            _.relationShipId == token.relationShipId && token.status != TokenStatus.Invalid
          )

        //tokens.forall(t => t.relationShipId != token.relationShipId || token.status == TokenStatus.Invalid)

        if (isEligible)
          Effect
            .persist(TokenAdded(token))
            .thenRun(_ => replyTo ! StatusReply.Success(TokenText(Token.encode(token))))
        else {
          replyTo ! StatusReply.Error(s"Token ${token.relationShipId.stringify} already exists")
          Effect.none[PartyAdded, State]
        }

      case InvalidateToken(relationShipId, replyTo) =>
        val isEligible: Boolean =
          checkTokens(state, relationShipId, t => t.relationShipId == relationShipId && t.status == TokenStatus.Waiting)

//          tokens.exists(t => t.relationShipId == relationShipId && t.status == TokenStatus.Waiting)

        if (isEligible)
          Effect
            .persist(TokenInvalidated(relationShipId))
            .thenRun(_ => replyTo ! StatusReply.Success(state))
        else {
          replyTo ! StatusReply.Error(s"Can't invalidate token for ${relationShipId.stringify}")
          Effect.none[TokenInvalidated, State]
        }

      case ConsumeToken(relationShipId, replyTo) =>
        val isEligible: Boolean =
          checkTokens(state, relationShipId, t => t.relationShipId == relationShipId && t.status == TokenStatus.Waiting)
//          tokens.exists(t => t.relationShipId == relationShipId && t.status == TokenStatus.Waiting)

        if (isEligible)
          Effect
            .persist(TokenConsumed(relationShipId))
            .thenRun(state => replyTo ! StatusReply.Success(state))
        else {
          replyTo ! StatusReply.Error(s"Can't consume token for ${relationShipId.stringify}")
          Effect.none[TokenConsumed, State]
        }

    }
  }

  private def checkTokens(state: State, relationShipId: RelationShipId, g: Token => Boolean): Boolean = {
    val tokens: List[Token] = state.tokens.getOrElse(relationShipId, List.empty[Token])
    tokens.exists(g)
  }

  val eventHandler: (State, Event) => State = (state, event) =>
    event match {
      case PartyAdded(party)                         => state.addParty(party)
      case PartyDeleted(party)                       => state.deleteParty(party)
      case PartyRelationShipAdded(partyRelationShip) => state.addPartyRelationShip(partyRelationShip)
      case PartyRelationShipDeleted(relationShipId)  => state.deletePartyRelationShip(relationShipId)
      case TokenAdded(relationShipId)                => state.addToken(relationShipId)
      case TokenInvalidated(relationShipId)          => state.invalidateToken(relationShipId)
      case TokenConsumed(relationShipId)             => state.consumeToken(relationShipId)
    }

  def apply(): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId("pdnd-interop-uservice-party-management"),
      emptyState = State.empty,
      commandHandler = commandHandler,
      eventHandler = eventHandler
    ).withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 10, keepNSnapshots = 1))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200 millis, 5 seconds, 0.1))
}
