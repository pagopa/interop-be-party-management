package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{InstitutionParty, Party, PersonParty}

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object PartyPersistentBehavior {

  final case class State(parties: Map[UUID, Party], indexes: Map[String, UUID]) extends CborSerializable {
    def add(party: Party): State = {
      copy(
        parties = parties + (party.id -> party),
        indexes = indexes + {
          party match {
            case p: PersonParty      => p.taxCode -> p.id
            case i: InstitutionParty => i.ipaCod  -> i.id
          }
        }
      )

    }

    def delete(party: Party): State = copy(
      parties = parties - party.id,
      indexes = indexes - {
        party match {
          case p: PersonParty      => p.taxCode
          case i: InstitutionParty => i.ipaCod
        }
      }
    )

  }

  object State {
    val empty: State = State(parties = Map.empty, indexes = Map.empty)
  }

  sealed trait Command extends CborSerializable

  final case class Add(entity: Party, replyTo: ActorRef[StatusReply[State]]) extends Command

  final case class Delete(entity: Party, replyTo: ActorRef[StatusReply[State]]) extends Command

  final case class Get(id: String, replyTo: ActorRef[StatusReply[Option[Party]]]) extends Command

  final case class List(replyTo: ActorRef[StatusReply[State]]) extends Command

  sealed trait Event extends CborSerializable

  final case class Added(party: Party) extends Event

  final case class Deleted(party: Party) extends Event

  val commandHandler: (State, Command) => Effect[Event, State] = { (state, command) =>
    command match {
      case Add(party, replyTo) =>
        Effect
          .persist(Added(party))
          .thenRun(state => {
            replyTo ! StatusReply.Success(state)
          })
      case Delete(party, replyTo) =>
        Effect
          .persist(Deleted(party))
          .thenRun(state => replyTo ! StatusReply.Success(state))
      case List(replyTo) =>
        replyTo ! StatusReply.Success(state)
        Effect.none
      case Get(id, replyTo) =>
        val party = for {
          uuid  <- state.indexes.get(id)
          party <- state.parties.get(uuid)
        } yield party

        replyTo ! StatusReply.Success(party)

        Effect.none
    }
  }

  val eventHandler: (State, Event) => State = (state, event) =>
    event match {
      case Added(party)   => state.add(party)
      case Deleted(party) => state.delete(party)
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
