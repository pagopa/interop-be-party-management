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
    tokens: Map[PartyRelationShipId, List[Token]],
    relationShips: Map[PartyRelationShipId, PartyRelationShip]
  ) extends CborSerializable {

    def addParty(party: Party): State =
      copy(parties = parties + (party.id -> party), indexes = indexes + (party.externalId -> party.id))

    def deleteParty(party: Party): State = copy(parties = parties - party.id, indexes = indexes - party.externalId)

    def addPartyRelationShip(partyRelationShip: PartyRelationShip): State =
      copy(relationShips = relationShips + (partyRelationShip.id -> partyRelationShip))

    def deletePartyRelationShip(partyRelationShipId: PartyRelationShipId): State =
      copy(relationShips = relationShips - partyRelationShipId)

    def addToken(token: Token): State = {
      tokens.get(token.partyRelationShipId) match {
        case Some(ts: List[Token]) => copy(tokens = tokens + (token.partyRelationShipId -> (token :: ts)))
        case None                  => copy(tokens = tokens + (token.partyRelationShipId -> (token :: Nil)))
      }
    }

    def invalidateToken(partyRelationShipId: PartyRelationShipId): State =
      changeTokenStatus(partyRelationShipId, Invalid)

    def consumeToken(partyRelationShipId: PartyRelationShipId): State =
      changeTokenStatus(partyRelationShipId, Consumed)

    private def changeTokenStatus(partyRelationShipId: PartyRelationShipId, status: TokenStatus): State = {
      val modified = tokens(partyRelationShipId).map {
        case x if x.partyRelationShipId == partyRelationShipId => x.copy(status = status)
        case x                                                 => x
      }
      copy(tokens = tokens + (partyRelationShipId -> modified))

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

  final case class DeletePartyRelationShip(
    partyRelationShipId: PartyRelationShipId,
    replyTo: ActorRef[StatusReply[State]]
  ) extends PartyRelationShipCommand

  final case class GetPartyRelationShip(
    partyRelationShipId: PartyRelationShipId,
    replyTo: ActorRef[StatusReply[Option[PartyRelationShip]]]
  ) extends PartyRelationShipCommand

  /* Party Command */
  final case class AddToken(token: Token, replyTo: ActorRef[StatusReply[TokenText]]) extends TokenCommand

  final case class InvalidateToken(partyRelationShipId: PartyRelationShipId, replyTo: ActorRef[StatusReply[State]])
      extends TokenCommand

  final case class ConsumeToken(partyRelationShipId: PartyRelationShipId, replyTo: ActorRef[StatusReply[State]])
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
  final case class PartyRelationShipAdded(partyRelationShip: PartyRelationShip)       extends PartyRelationShipEvent
  final case class PartyRelationShipDeleted(partyRelationShipId: PartyRelationShipId) extends PartyRelationShipEvent

  /* Token Event */
  final case class TokenAdded(token: Token)                                   extends TokenEvent
  final case class TokenInvalidated(partyRelationShipId: PartyRelationShipId) extends TokenEvent
  final case class TokenConsumed(partyRelationShipId: PartyRelationShipId)    extends TokenEvent

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
        state.indexes.foreach(println)
        val party: Option[ApiParty] = for {
          uuid <- state.indexes.get(id)
          _ = logger.info(s"Found $id/${uuid.toString}")
          party <- state.parties.get(uuid)
        } yield convertToApi(party)

        replyTo ! StatusReply.Success(party)

        Effect.none

      case AddPartyRelationShip(from, to, role, replyTo) =>
        val partyRelationShip: PartyRelationShip = PartyRelationShip.create(from, to, role)
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

      case DeletePartyRelationShip(partyRelationShipId, replyTo) =>
        Effect
          .persist(PartyRelationShipDeleted(partyRelationShipId))
          .thenRun(state => replyTo ! StatusReply.Success(state))

      case GetPartyRelationShip(partyRelationShipId, replyTo) =>
        val partyRelationShip: Option[PartyRelationShip] = state.relationShips.get(partyRelationShipId)

        replyTo ! StatusReply.Success(partyRelationShip)

        Effect.none

      case AddToken(token, replyTo) =>
        val tokens: List[Token] = state.tokens.getOrElse(token.partyRelationShipId, List.empty[Token])

        val isInvalidToken: Boolean = tokens.exists(token => Set[TokenStatus](Consumed, Waiting).contains(token.status))

        if (isInvalidToken) {
          replyTo ! StatusReply.Error(s"Token ${token.partyRelationShipId.stringify} already exists")
          Effect.none[PartyAdded, State]
        } else
          Effect
            .persist(TokenAdded(token))
            .thenRun(_ => replyTo ! StatusReply.Success(TokenText(Token.encode(token))))

      case InvalidateToken(partyRelationShipId, replyTo) =>
        Effect
          .persist(TokenInvalidated(partyRelationShipId))
          .thenRun(state => replyTo ! StatusReply.Success(state))

      case ConsumeToken(partyRelationShipId, replyTo) =>
        Effect
          .persist(TokenConsumed(partyRelationShipId))
          .thenRun(state => replyTo ! StatusReply.Success(state))
    }
  }

  val eventHandler: (State, Event) => State = (state, event) =>
    event match {
      case PartyAdded(party)                             => state.addParty(party)
      case PartyDeleted(party)                           => state.deleteParty(party)
      case PartyRelationShipAdded(partyRelationShip)     => state.addPartyRelationShip(partyRelationShip)
      case PartyRelationShipDeleted(partyRelationShipId) => state.deletePartyRelationShip(partyRelationShipId)
      case TokenAdded(partyRelationShipId)               => state.addToken(partyRelationShipId)
      case TokenInvalidated(partyRelationShipId)         => state.invalidateToken(partyRelationShipId)
      case TokenConsumed(partyRelationShipId)            => state.consumeToken(partyRelationShipId)
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
