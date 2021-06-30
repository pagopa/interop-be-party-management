package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EffectBuilder, EventSourcedBehavior, RetentionCriteria}
import cats.implicits.toTraverseOps
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.PartyRelationShipStatus.Active
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{RelationShip, TokenText}
import org.slf4j.LoggerFactory

import java.time.temporal.ChronoUnit
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.language.postfixOps

object PartyPersistentBehavior {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def commandHandler(
    shard: ActorRef[ClusterSharding.ShardCommand],
    context: ActorContext[Command]
  ): (State, Command) => Effect[Event, State] = { (state, command) =>
    val idleTimeout =
      context.system.settings.config.getDuration("uservice-party-management.idle-timeout")
    context.setReceiveTimeout(idleTimeout.get(ChronoUnit.SECONDS) seconds, Idle)
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
              .thenRun(_ => replyTo ! StatusReply.Success(party))
          }

      case DeleteParty(party, replyTo) =>
        Effect
          .persist(PartyDeleted(party))
          .thenRun(state => replyTo ! StatusReply.Success(state))

      case GetParty(id, replyTo) =>
        logger.info(s"Getting party $id")

        val party: Option[Party] = for {
          uuid <- state.indexes.get(id)
          _ = logger.info(s"Found $id/${uuid.toString}")
          party <- state.parties.get(uuid)
        } yield party

        replyTo ! party

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
                  s"Something goes wrong trying to update attributes for party $organizationId: ${ex.getMessage}"
                )
                Effect.none[AttributesAdded, State]
              },
              p => {
                Effect
                  .persist(AttributesAdded(p))
                  .thenRun(_ => replyTo ! StatusReply.Success(p))
              }
            )
          }
          .getOrElse {
            replyTo ! StatusReply.Error(s"Party $organizationId not found")
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

      case GetPartyRelationShips(from, replyTo) =>
        val relationShips: List[RelationShip] =
          state.relationShips.filter(_._1.from == from).values.toList.flatMap { rl =>
            for {
              from <- state.parties.get(rl.id.from)
              to   <- state.parties.get(rl.id.to)
            } yield RelationShip(
              from = from.externalId,
              to = to.externalId,
              role = rl.id.role.stringify,
              status = Some(rl.status.stringify)
            )
          }
        replyTo ! StatusReply.Success(relationShips)

        Effect.none

      case GetPartyRelationShip(from, to, replyTo) =>
        val partyRelationShip: Option[PartyRelationShip] =
          state.relationShips
            .find(relationShip => relationShip._1.from == from && relationShip._1.to == to)
            .map(_._2)

        val relationShip: Option[RelationShip] = partyRelationShip.flatMap { rl =>
          for {
            from <- state.parties.get(rl.id.from)
            to   <- state.parties.get(rl.id.to)
          } yield RelationShip(
            from = from.externalId,
            to = to.externalId,
            role = rl.id.role.stringify,
            status = Some(rl.status.stringify)
          )
        }
        replyTo ! StatusReply.Success(relationShip)

        Effect.none

      case AddToken(tokenSeed, replyTo) =>
        val parties: Either[RuntimeException, Seq[PartyRelationShipId]] =
          tokenSeed.relationShips.items
            .traverse(relationShip =>
              for {
                fromIdx <- state.indexes.get(relationShip.from)
                from    <- state.parties.get(fromIdx)
                toIdx   <- state.indexes.get(relationShip.to)
                to      <- state.parties.get(toIdx)
                role    <- PartyRole.fromText(relationShip.role).toOption
              } yield PartyRelationShipId(from.id, to.id, role)
            )
            .toRight(new RuntimeException(s"Parties found"))

        val token: Either[Throwable, Token] = parties.flatMap(pts => Token.generate(tokenSeed, pts))

        token match {
          case Right(tk) =>
            val itCanBeInsert: Boolean =
              state.tokens.get(tk.id).exists(t => t.isValid && t.status == Invalid) || !state.tokens.contains(tk.id)

            if (itCanBeInsert) {
              Effect
                .persist(TokenAdded(tk))
                .thenRun(_ => replyTo ! StatusReply.Success(TokenText(Token.encode(tk))))
            } else {
              replyTo ! StatusReply.Error(s"Invalid token status ${tk.status.toString}: token seed ${tk.seed.toString}")
              Effect.none[TokenAdded, State]
            }
          case Left(ex) =>
            replyTo ! StatusReply.Error(s"Token creation failed due: ${ex.getMessage}")
            Effect.none[TokenAdded, State]
        }

      case VerifyToken(token, replyTo) =>
        val verified: Option[Token] = state.tokens.get(token.id)
        replyTo ! StatusReply.Success(verified)
        Effect.none

      case InvalidateToken(token, replyTo) =>
        processToken(state.tokens.get(token.id), replyTo, state, TokenInvalidated)

      case ConsumeToken(token, replyTo) =>
        processToken(state.tokens.get(token.id), replyTo, state, TokenConsumed)

      case Idle =>
        shard ! ClusterSharding.Passivate(context.self)
        context.log.error(s"Passivate shard: ${shard.path.name}")
        Effect.none[Event, State]
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

  val TypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("uservice-party-management-persistence-party")

  def apply(shard: ActorRef[ClusterSharding.ShardCommand], persistenceId: PersistenceId): Behavior[Command] = {
    Behaviors.setup { context =>
      context.log.error(s"Starting EService Shard ${persistenceId.id}")
      val numberOfEvents =
        context.system.settings.config
          .getInt("uservice-party-management.number-of-events-before-snapshot")
      EventSourcedBehavior[Command, Event, State](
        persistenceId = persistenceId,
        emptyState = State.empty,
        commandHandler = commandHandler(shard, context),
        eventHandler = eventHandler
      ).withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = numberOfEvents, keepNSnapshots = 1))
        .withTagger(_ => Set(persistenceId.id))
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(200 millis, 5 seconds, 0.1))
    }
  }

}
