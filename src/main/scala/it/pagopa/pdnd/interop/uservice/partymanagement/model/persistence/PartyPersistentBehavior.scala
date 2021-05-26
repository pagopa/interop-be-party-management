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

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object PartyPersistentBehavior {

  private val logger = LoggerFactory.getLogger(this.getClass)

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

      case AddAttributes(organizationId, attributeRecords, replyTo) =>
        val party: Option[Party] = for {
          uuid <- state.indexes.get(organizationId)
          _ = logger.info(s"Found $organizationId/${uuid.toString}")
          party <- state.parties.get(uuid)
        } yield party

        val attributes: Set[Attributes] = attributeRecords.map(Attributes.fromApi).toSet
        party
          .map { p =>
            val updated: Either[Throwable, Party] = Party.addAttributes(p, attributes)
            updated.fold[Effect[AttributesAdded, State]](
              ex => {
                replyTo ! StatusReply.Error(
                  s"Something goes wrong trying to update attributes for party ${organizationId}: ${ex.getMessage}"
                )
                Effect.none[AttributesAdded, State]
              },
              p => {
                val party: ApiParty = Party.convertToApi(p)
                Effect
                  .persist(AttributesAdded(p))
                  .thenRun(_ => replyTo ! StatusReply.Success(party))
              }
            )
          }
          .getOrElse {
            replyTo ! StatusReply.Error(s"Party ${organizationId} not found")
            Effect.none[AttributesAdded, State]
          }

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
        val relationShips: List[RelationShip] =
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
        replyTo ! StatusReply.Success(relationShips)

        Effect.none

      case AddToken(token, replyTo) =>
        state.tokens.get(token.seed) match {
          case Some(t) if t.status == Invalid =>
            Effect
              .persist(TokenAdded(token))
              .thenRun(_ => replyTo ! StatusReply.Success(TokenText(Token.encode(token))))
          case None =>
            Effect
              .persist(TokenAdded(token))
              .thenRun(_ => replyTo ! StatusReply.Success(TokenText(Token.encode(token))))
          case Some(t) =>
            replyTo ! StatusReply.Error(s"Invalid token status ${t.status.toString}: token seed ${t.seed.toString}")
            Effect.none[TokenAdded, State]
        }

      case VerifyToken(token, replyTo) =>
        val verified: Option[Token] = state.tokens.get(token.seed)
        replyTo ! StatusReply.Success(verified)
        Effect.none

      case InvalidateToken(token, replyTo) =>
        processToken(state.tokens.get(token.seed), replyTo, state, TokenInvalidated)

      case ConsumeToken(token, replyTo) =>
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
      case Some(t) if t.status == Waiting =>
        Effect
          .persist(event(t))
          .thenRun(_ => replyTo ! StatusReply.Success(output))
      case Some(t) =>
        replyTo ! StatusReply.Error(s"Invalid token status ${t.status.toString}: token seed ${t.seed.toString}")
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
      case AttributesAdded(party)                    => state.updateParty(party)
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
    ).withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 1000, keepNSnapshots = 1))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200 millis, 5 seconds, 0.1))
}
