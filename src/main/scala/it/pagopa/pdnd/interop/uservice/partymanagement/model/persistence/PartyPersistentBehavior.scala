package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.TokenText
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party.PartyRelationShipStatus.Active
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import org.slf4j.LoggerFactory

import java.time.temporal.ChronoUnit
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.language.postfixOps
@SuppressWarnings(
  Array(
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.StringPlusAny",
    "org.wartremover.warts.Equals",
    "org.wartremover.warts.ToString"
  )
)
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
      case AddParty(party, shardId, replyTo) =>
        logger.error(s"Adding party ${party.externalId} to shard $shardId")
        logger.error(state.toString)
        state.indexes
          .get(party.externalId)
          .map { _ =>
            logger.error(s"AddParty found party ${party.externalId} at shard $shardId")
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
          .thenRun(_ => replyTo ! StatusReply.Success(()))

      case GetParty(uuid, replyTo) =>
        val party: Option[Party] = state.parties.get(uuid)
        party.foreach(p => logger.info(s"Found party ${p.externalId}/${p.id.toString}"))
        replyTo ! party

        Effect.none

      case GetPartyByExternalId(externalId, shardId, replyTo) =>
        val party: Option[Party] = for {
          uuid <- state.indexes.get(externalId)
          _ = logger.info(s"GetPartyByExternalId found $externalId/${uuid.toString} at shard $shardId")
          party <- state.parties.get(uuid)
        } yield party

        replyTo ! party

        Effect.none

      case AddAttributes(organizationId, attributes, replyTo) =>
        val party: Option[Party] = for {
          uuid <- state.indexes.get(organizationId)
          _ = logger.info(s"Found $organizationId/${uuid.toString}")
          party <- state.parties.get(uuid)
        } yield party

        party
          .map { p =>
            val updated: Either[Throwable, Party] = p.addAttributes(attributes.toSet)
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
            Effect.none[PartyRelationShipAdded, State]
          }
          .getOrElse {
            if (isEligible || Set[PartyRole](Manager, Delegate).contains(role))
              Effect
                .persist(PartyRelationShipAdded(partyRelationShip))
                .thenRun(_ => replyTo ! StatusReply.Success(()))
            else {
              replyTo ! StatusReply.Error(s"Operator without manager")
              Effect.none[PartyRelationShipAdded, State]
            }
          }

      case ConfirmPartyRelationShip(partyRelationShipId, replyTo) =>
        state.relationShips
          .get(partyRelationShipId)
          .fold {
            replyTo ! StatusReply.Error(s"Relationship ${partyRelationShipId.stringify} not found")
            Effect.none[PartyRelationShipConfirmed, State]
          } { t =>
            Effect
              .persist(PartyRelationShipConfirmed(t.id))
              .thenRun(_ => replyTo ! StatusReply.Success(()))
          }

      case DeletePartyRelationShip(partyRelationShipId, replyTo) =>
        Effect
          .persist(PartyRelationShipDeleted(partyRelationShipId))
          .thenRun(_ => replyTo ! StatusReply.Success(()))

      case GetPartyRelationShips(from, replyTo) =>
        val relationShips: List[PartyRelationShip] = state.relationShips.filter(_._1.from == from).values.toList
        replyTo ! relationShips
        Effect.none

      case AddToken(tokenSeed, partyRelationShipIds, replyTo) =>
        val token: Either[Throwable, Token] = Token.generate(tokenSeed, partyRelationShipIds)

        token match {
          case Right(tk) =>
            val itCanBeInsert: Boolean =
              state.tokens.get(tk.id).exists(t => t.isValid) || !state.tokens.contains(tk.id)

            if (itCanBeInsert) {
              Effect
                .persist(TokenAdded(tk))
                .thenRun(_ => replyTo ! StatusReply.Success(TokenText(Token.encode(tk))))
            } else {
              replyTo ! StatusReply.Error(s"Token is expired: token seed ${tk.seed.toString}")
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

      case DeleteToken(token, replyTo) =>
        Effect
          .persist(TokenDeleted(token))
          .thenRun(_ => replyTo ! StatusReply.Success(()))

      case Idle =>
        shard ! ClusterSharding.Passivate(context.self)
//        context.log.error(s"Passivate shard: ${shard.path.name}")
        Effect.none[Event, State]
    }

  }

  val eventHandler: (State, Event) => State = (state, event) =>
    event match {
      case PartyAdded(party)                          => state.addParty(party)
      case PartyDeleted(party)                        => state.deleteParty(party)
      case AttributesAdded(party)                     => state.updateParty(party)
      case PartyRelationShipAdded(partyRelationShip)  => state.addPartyRelationShip(partyRelationShip)
      case PartyRelationShipConfirmed(relationShipId) => state.confirmPartyRelationShip(relationShipId)
      case PartyRelationShipDeleted(relationShipId)   => state.deletePartyRelationShip(relationShipId)
      case TokenAdded(token)                          => state.addToken(token)
      case TokenDeleted(token)                        => state.deleteToken(token)
    }

  val TypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("uservice-party-management-persistence-party")

  def apply(shard: ActorRef[ClusterSharding.ShardCommand], persistenceId: PersistenceId): Behavior[Command] = {
    Behaviors.setup { context =>
//      context.log.error(s"Starting EService Shard ${persistenceId.id}")
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
