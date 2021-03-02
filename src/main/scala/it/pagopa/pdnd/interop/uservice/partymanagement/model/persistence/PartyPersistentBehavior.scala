package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.Party

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object PartyPersistentBehavior {

  final case class State(parties: Map[UUID, Party]) extends CborSerializable {
    def add(party: Party): State = copy(parties = parties + (party.id -> party))

    def delete(id: UUID): State = copy(parties = parties - id)

  }

  object State {
    val empty: State = State(parties = Map.empty)
  }

  sealed trait Command extends CborSerializable

  final case class Add(entity: Party, replyTo: ActorRef[StatusReply[State]]) extends Command

  final case class Delete(id: UUID, replyTo: ActorRef[StatusReply[State]]) extends Command

  final case class Get(id: UUID, replyTo: ActorRef[StatusReply[Option[Party]]]) extends Command

  final case class List(replyTo: ActorRef[StatusReply[State]]) extends Command

  sealed trait Event extends CborSerializable

  final case class Added(party: Party) extends Event

  final case class Deleted(id: UUID) extends Event

  val commandHandler: (State, Command) => Effect[Event, State] = { (state, command) =>
    command match {
      case Add(party, replyTo) =>
        Effect
          .persist(Added(party))
          .thenRun(state => {
            replyTo ! StatusReply.Success(state)
          })
      case Delete(id, replyTo) =>
        Effect
          .persist(Deleted(id))
          .thenRun(state => replyTo ! StatusReply.Success(state))
      case List(replyTo) =>
        replyTo ! StatusReply.Success(state)
        Effect.none
      case Get(id, replyTo) =>
        replyTo ! StatusReply.Success(state.parties.get(id))
        Effect.none
    }
  }

  val eventHandler: (State, Event) => State = (state, event) =>
    event match {
      case Added(party) =>
        state.add(party)
      case Deleted(id) =>
        state.delete(id)
    }

  def apply(): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId("pdnd-interop-uservice-party-management-party"),
      emptyState = State.empty,
      commandHandler = commandHandler,
      eventHandler = eventHandler
    ).withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 10, keepNSnapshots = 1))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200 millis, 5 seconds, 0.1))
}
