package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.TokenText
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
      case AddParty(party, replyTo) =>
        logger.error(s"Adding party ${party.id}")
        logger.error(state.toString)
        state.parties
          .get(party.id)
          .map { _ =>
            logger.error(s"AddParty found party ${party.id}")
            replyTo ! StatusReply.Error(s"Party ${party.id} already exists")
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
        party.foreach(p => logger.info(s"Found party ${p.id.toString}"))
        replyTo ! party

        Effect.none

      case GetParties(uuids, replyTo) =>
        val foundParties: Seq[Party] = {
          uuids.flatMap(uuid => state.parties.get(uuid))
        }
        foundParties.foreach(p => logger.info(s"Found party ${p.id.toString}"))
        replyTo ! foundParties
        Effect.none

      case GetPartyAttributes(uuid, replyTo) =>
        val statusReply: StatusReply[Seq[String]] = state.parties
          .get(uuid)
          .map {
            case institutionParty: InstitutionParty => StatusReply.success(institutionParty.attributes.toSeq)
            case _: PersonParty                     => StatusReply.success(Seq.empty[String])
          }
          .getOrElse {
            StatusReply.Error[Seq[String]](s"Party $uuid not found")
          }

        replyTo ! statusReply
        Effect.none

      case GetOrganizationByExternalId(externalId, replyTo) =>
        val party: Option[InstitutionParty] = state.parties.collectFirst {
          case (_, o: InstitutionParty) if o.externalId == externalId => o
        }
        party.foreach(p => logger.info(s"Found organization ${p.id.toString}"))
        replyTo ! party

        Effect.none

      case AddAttributes(organizationId, attributes, replyTo) =>
        state.parties
          .get(organizationId)
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

      case AddPartyRelationship(partyRelationship, replyTo) =>
        state.relationships
          .get(partyRelationship.id.toString)
          .map { _ =>
            replyTo ! StatusReply.Error(s"Relationship ${partyRelationship.id.toString} already exists")
            Effect.none[PartyRelationshipAdded, State]
          }
          .getOrElse {

            Effect
              .persist(PartyRelationshipAdded(partyRelationship))
              .thenRun(_ => replyTo ! StatusReply.Success(()))

          }

      case ConfirmPartyRelationship(partyRelationshipId, filePath, fileInfo, replyTo) =>
        state.relationships
          .get(partyRelationshipId.toString)
          .fold {
            replyTo ! StatusReply.Error(s"Relationship ${partyRelationshipId.toString} not found")
            Effect.none[PartyRelationshipConfirmed, State]
          } { t =>
            Effect
              .persist(
                PartyRelationshipConfirmed(t.id, filePath, fileInfo.getFileName, fileInfo.getContentType.toString())
              )
              .thenRun(_ => replyTo ! StatusReply.Success(()))
          }

      case DeletePartyRelationship(partyRelationshipId, replyTo) =>
        Effect
          .persist(PartyRelationshipDeleted(partyRelationshipId))
          .thenRun(_ => replyTo ! StatusReply.Success(()))

      case SuspendPartyRelationship(partyRelationshipId, replyTo) =>
        val relationship: Option[PartyRelationship] = state.relationships.get(partyRelationshipId.toString)

        relationship match {
          case Some(rel) =>
            Effect
              .persist(PartyRelationshipSuspended(rel.id))
              .thenRun(_ => replyTo ! StatusReply.Success(()))
          case None =>
            replyTo ! StatusReply.Error(s"Relationship ${partyRelationshipId.toString} not found")
            Effect.none
        }

      case ActivatePartyRelationship(partyRelationshipId, replyTo) =>
        val relationship: Option[PartyRelationship] = state.relationships.get(partyRelationshipId.toString)

        relationship match {
          case Some(rel) =>
            Effect
              .persist(PartyRelationshipActivated(rel.id))
              .thenRun(_ => replyTo ! StatusReply.Success(()))
          case None =>
            replyTo ! StatusReply.Error(s"Relationship ${partyRelationshipId.toString} not found")
            Effect.none
        }

      case GetPartyRelationshipById(uuid, replyTo) =>
        val relationship: Option[PartyRelationship] = state.relationships.get(uuid.toString)
        replyTo ! relationship
        Effect.none

      case GetPartyRelationshipsByFrom(from, replyTo) =>
        val relationships: List[PartyRelationship] = state.relationships.values.filter(_.from == from).toList
        replyTo ! relationships
        Effect.none

      case GetPartyRelationshipsByTo(to, replyTo) =>
        val relationships: List[PartyRelationship] = state.relationships.values.filter(_.to == to).toList
        replyTo ! relationships
        Effect.none

      case AddToken(tokenSeed, partyRelationshipIds, replyTo) =>
        val token: Either[Throwable, Token] = Token.generate(tokenSeed, partyRelationshipIds)

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

      case GetPartyRelationshipByAttributes(from, to, role, platformRole, replyTo) =>
        replyTo ! state.getPartyRelationshipByAttributes(from, to, role, platformRole)
        Effect.none[Event, State]

      case Idle =>
        shard ! ClusterSharding.Passivate(context.self)
//        context.log.error(s"Passivate shard: ${shard.path.name}")
        Effect.none[Event, State]
    }

  }

  val eventHandler: (State, Event) => State = (state, event) =>
    event match {
      case PartyAdded(party)                         => state.addParty(party)
      case PartyDeleted(party)                       => state.deleteParty(party)
      case AttributesAdded(party)                    => state.updateParty(party)
      case PartyRelationshipAdded(partyRelationship) => state.addPartyRelationship(partyRelationship)
      case PartyRelationshipConfirmed(relationshipId, filePath, fileName, contentType) =>
        state.confirmPartyRelationship(relationshipId, filePath, fileName, contentType)
      case PartyRelationshipDeleted(relationshipId)   => state.deletePartyRelationship(relationshipId)
      case PartyRelationshipSuspended(relationshipId) => state.suspendRelationship(relationshipId)
      case PartyRelationshipActivated(relationshipId) => state.activateRelationship(relationshipId)
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
