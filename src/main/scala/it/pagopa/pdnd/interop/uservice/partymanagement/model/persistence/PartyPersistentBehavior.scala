package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.{
  InstitutionParty,
  Party,
  PartyRelationShip,
  PartyRelationShipId,
  PartyRole,
  PersonParty
}

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object PartyPersistentBehavior {

  final case class State(
    parties: Map[UUID, Party],
    indexes: Map[String, UUID],
    relationShips: Map[PartyRelationShipId, PartyRelationShip]
  ) extends CborSerializable {
    def addParty(party: Party): State = {
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

    def deleteParty(party: Party): State = copy(
      parties = parties - party.id,
      indexes = indexes - {
        party match {
          case p: PersonParty      => p.taxCode
          case i: InstitutionParty => i.ipaCod
        }
      }
    )

    def addPartyRelationShip(partyRelationShip: PartyRelationShip): State =
      copy(relationShips = relationShips + (partyRelationShip.id -> partyRelationShip))

    def deletePartyRelationShip(partyRelationShip: PartyRelationShip): State =
      copy(relationShips = relationShips - partyRelationShip.id)

  }

  object State {
    val empty: State = State(parties = Map.empty, indexes = Map.empty, relationShips = Map.empty)
  }

  /* Command */
  sealed trait Command                  extends CborSerializable
  sealed trait PartyCommand             extends Command
  sealed trait PartyRelationShipCommand extends Command

  /* Party Command */
  final case class AddParty(entity: Party, replyTo: ActorRef[StatusReply[State]])      extends PartyCommand
  final case class DeleteParty(entity: Party, replyTo: ActorRef[StatusReply[State]])   extends PartyCommand
  final case class GetParty(id: String, replyTo: ActorRef[StatusReply[Option[Party]]]) extends PartyCommand

  /* PartyRelationShip Command */
  final case class AddPartyRelationShip(
    from: Party,
    to: Party,
    partyRole: PartyRole,
    replyTo: ActorRef[StatusReply[State]]
  ) extends PartyRelationShipCommand

  final case class DeletePartyRelationShip(
    partyRelationShipId: PartyRelationShipId,
    replyTo: ActorRef[StatusReply[State]]
  ) extends PartyRelationShipCommand

  final case class GetPartyRelationShip(
    partyRelationShipId: PartyRelationShipId,
    replyTo: ActorRef[StatusReply[Option[Party]]]
  ) extends PartyRelationShipCommand

  /* Event */
  sealed trait Event                  extends CborSerializable
  sealed trait PartyEvent             extends Event
  sealed trait PartyRelationShipEvent extends Event

  /* Party Event */
  final case class PartyAdded(party: Party)   extends PartyEvent
  final case class PartyDeleted(party: Party) extends PartyEvent

  /* PartyRelationShip Event */
  final case class PartyRelationShipAdded(partyRelationShip: PartyRelationShip)   extends PartyRelationShipEvent
  final case class PartyRelationShipDeleted(partyRelationShip: PartyRelationShip) extends PartyRelationShipEvent

  val commandHandler: (State, Command) => Effect[Event, State] = { (state, command) =>
    command match {
      case AddParty(party, replyTo) =>
        Effect
          .persist(PartyAdded(party))
          .thenRun(state => {
            replyTo ! StatusReply.Success(state)
          })
      case DeleteParty(party, replyTo) =>
        Effect
          .persist(PartyDeleted(party))
          .thenRun(state => replyTo ! StatusReply.Success(state))
      case GetParty(id, replyTo) =>
        val party: Option[Party] = for {
          uuid  <- state.indexes.get(id)
          party <- state.parties.get(uuid)
        } yield party

        replyTo ! StatusReply.Success(party)

        Effect.none
      case AddPartyRelationShip()  => ???
      case DeletePartyRelationShip => ???
      case GetPartyRelationShip    => ???
    }
  }

  val eventHandler: (State, Event) => State = (state, event) =>
    event match {
      case PartyAdded(party)                           => state.addParty(party)
      case PartyDeleted(party)                         => state.deleteParty(party)
      case PartyRelationShipAdded(partyRelationShip)   => state.addPartyRelationShip(partyRelationShip)
      case PartyRelationShipDeleted(partyRelationShip) => state.deletePartyRelationShip(partyRelationShip)
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
