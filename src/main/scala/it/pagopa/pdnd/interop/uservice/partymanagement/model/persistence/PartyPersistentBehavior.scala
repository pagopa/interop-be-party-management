package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EffectBuilder, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.ApiParty
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.Party.convertToApi
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.PartyRelationShipStatus.Active
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{RelationShip, TokenText}
import org.slf4j.LoggerFactory

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object PartyPersistentBehavior {

  private val logger = LoggerFactory.getLogger(this.getClass)

  final case class State(
    parties: Map[UUID, Party],
    indexes: Map[String, UUID],
    tokens: Map[UUID, Token],
    relationShips: Map[PartyRelationShipId, PartyRelationShip]
  ) extends CborSerializable {

    def addParty(party: Party): State =
      copy(parties = parties + (party.id -> party), indexes = indexes + (party.externalId -> party.id))

    def deleteParty(party: Party): State = copy(parties = parties - party.id, indexes = indexes - party.externalId)

    def addPartyRelationShip(relationShip: PartyRelationShip): State =
      copy(relationShips = relationShips + (relationShip.id -> relationShip))

    def deletePartyRelationShip(relationShipId: PartyRelationShipId): State =
      copy(relationShips = relationShips - relationShipId)

    def addToken(token: Token): State = {
      copy(tokens = tokens + (token.seed -> token))
    }

    def invalidateToken(token: Token): State =
      changeTokenStatus(token, TokenStatus.Invalid)

    def consumeToken(token: Token): State =
      changeTokenStatus(token, TokenStatus.Consumed)

    private def changeTokenStatus(token: Token, status: TokenStatus): State = {
      val modified = tokens.get(token.seed).map(t => t.copy(status = status))

      modified match {
        case Some(t) if status == TokenStatus.Consumed =>
          val managerRelationShip  = relationShips(token.manager).copy(status = PartyRelationShipStatus.Active)
          val delegateRelationShip = relationShips(token.delegate).copy(status = PartyRelationShipStatus.Active)
          copy(
            relationShips =
              relationShips ++ Map(token.manager -> managerRelationShip, token.delegate -> delegateRelationShip),
            tokens = tokens + (t.seed -> t)
          )
        case Some(t) => copy(tokens = tokens + (t.seed -> t))
        case None    => this
      }

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

  final case class DeletePartyRelationShip(relationShipId: PartyRelationShipId, replyTo: ActorRef[StatusReply[State]])
      extends PartyRelationShipCommand

  final case class GetPartyRelationShip(taxCode: UUID, replyTo: ActorRef[StatusReply[List[RelationShip]]])
      extends PartyRelationShipCommand

  /* Party Command */
  final case class AddToken(token: Token, replyTo: ActorRef[StatusReply[TokenText]]) extends TokenCommand

  final case class InvalidateToken(token: Token, replyTo: ActorRef[StatusReply[State]]) extends TokenCommand

  final case class ConsumeToken(token: Token, replyTo: ActorRef[StatusReply[State]]) extends TokenCommand

  /* Event */
  sealed trait Event                  extends CborSerializable
  sealed trait PartyEvent             extends Event
  sealed trait PartyRelationShipEvent extends Event
  sealed trait TokenEvent             extends Event

  /* Party Event */
  final case class PartyAdded(party: Party)   extends PartyEvent
  final case class PartyDeleted(party: Party) extends PartyEvent

  /* PartyRelationShip Event */
  final case class PartyRelationShipAdded(partyRelationShip: PartyRelationShip)  extends PartyRelationShipEvent
  final case class PartyRelationShipDeleted(relationShipId: PartyRelationShipId) extends PartyRelationShipEvent

  /* Token Event */
  final case class TokenAdded(token: Token)       extends TokenEvent
  final case class TokenInvalidated(token: Token) extends TokenEvent
  final case class TokenConsumed(token: Token)    extends TokenEvent

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
        val isEligible = state.relationShips.exists(p => p._1.to == to && p._1.role == Manager && p._2.status == Active)

        val partyRelationShip: PartyRelationShip = PartyRelationShip.create(from, to, role)

        state.relationShips
          .get(partyRelationShip.id)
          .map { _ =>
            replyTo ! StatusReply.Error(s"Relationship ${partyRelationShip.id.stringify} already exists")
            Effect.none[PartyAdded, State]
          }
          .getOrElse {
            if (isEligible || Set[PartyRole](Manager, Delegate).contains(role))
              Effect
                .persist(PartyRelationShipAdded(partyRelationShip))
                .thenRun(state => replyTo ! StatusReply.Success(state))
            else {
              replyTo ! StatusReply.Error(s"Operator without manager")
              Effect.none[PartyAdded, State]
            }
          }

      case DeletePartyRelationShip(relationShipId, replyTo) =>
        Effect
          .persist(PartyRelationShipDeleted(relationShipId))
          .thenRun(state => replyTo ! StatusReply.Success(state))

      case GetPartyRelationShip(from, replyTo) =>
        val partyRelationShip: List[RelationShip] =
          state.relationShips.filter(_._1.from == from).values.toList.flatMap { rl =>
            for {
              from <- state.parties.get(rl.id.from)
              to   <- state.parties.get(rl.id.to)
            } yield RelationShip(
              taxCode = from.externalId,
              institutionId = to.externalId,
              role = rl.id.role.stringify,
              status = rl.status.stringify
            )
          }
        replyTo ! StatusReply.Success(partyRelationShip)

        Effect.none

      case AddToken(token, replyTo) =>
        state.tokens.get(token.seed) match {
          case Some(t) if t.status == TokenStatus.Invalid =>
            Effect
              .persist(TokenAdded(token))
              .thenRun(_ => replyTo ! StatusReply.Success(TokenText(Token.encode(token))))
          case None =>
            Effect
              .persist(TokenAdded(token))
              .thenRun(_ => replyTo ! StatusReply.Success(TokenText(Token.encode(token))))
          case Some(t) =>
            replyTo ! StatusReply.Error(s"Invalid token status ${t.status}: token seed ${t.seed}")
            Effect.none[TokenAdded, State]
        }

      case InvalidateToken(token, replyTo) =>
//        state.tokens.get(token.seed) match {
//          case Some(t) if t.status == TokenStatus.Waiting =>
//            Effect
//              .persist(TokenInvalidated(token))
//              .thenRun(_ => replyTo ! StatusReply.Success(state))
//          case Some(t) =>
//            replyTo ! StatusReply.Error(s"Invalid token status: token seed ${t.seed}")
//            Effect.none[TokenInvalidated, State]
//          case None =>
//            replyTo ! StatusReply.Error(s"Token ${token.seed} not found")
//            Effect.none[TokenInvalidated, State]
//        }
        processToken(state.tokens.get(token.seed), replyTo, state, TokenInvalidated)
      case ConsumeToken(token, replyTo) =>
//        state.tokens.get(token.seed) match {
//          case Some(t) if t.status == TokenStatus.Waiting =>
//            Effect
//              .persist(TokenConsumed(token))
//              .thenRun(_ => replyTo ! StatusReply.Success(state))
//          case Some(t) =>
//            replyTo ! StatusReply.Error(s"Invalid token status: token seed ${t.seed}")
//            Effect.none[TokenConsumed, State]
//          case None =>
//            replyTo ! StatusReply.Error(s"Token ${token.seed} not found")
//            Effect.none[TokenConsumed, State]
//        }
        processToken(state.tokens.get(token.seed), replyTo, state, TokenConsumed)
    }

  }
  private def processToken[A](
    token: Option[Token],
    replyTo: ActorRef[StatusReply[A]],
    output: A,
    event: Token => Event
  ): EffectBuilder[Event, State] = {
    token match {
      case Some(t) if t.status == TokenStatus.Waiting =>
        Effect
          .persist(event(t))
          .thenRun(_ => replyTo ! StatusReply.Success(output))
      case Some(t) =>
        replyTo ! StatusReply.Error(s"Invalid token status ${t.status}: token seed ${t.seed}")
        Effect.none[Event, State]
      case None =>
        replyTo ! StatusReply.Error(s"Token not found")
        Effect.none[Event, State]
    }
  }

  val eventHandler: (State, Event) => State = (state, event) =>
    event match {
      case PartyAdded(party)                         => state.addParty(party)
      case PartyDeleted(party)                       => state.deleteParty(party)
      case PartyRelationShipAdded(partyRelationShip) => state.addPartyRelationShip(partyRelationShip)
      case PartyRelationShipDeleted(relationShipId)  => state.deletePartyRelationShip(relationShipId)
      case TokenAdded(token)                         => state.addToken(token)
      case TokenInvalidated(token)                   => state.invalidateToken(token)
      case TokenConsumed(token)                      => state.consumeToken(token)
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
