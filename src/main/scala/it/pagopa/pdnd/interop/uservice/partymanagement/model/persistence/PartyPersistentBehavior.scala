package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.pdnd.interop.commons.utils.OpenapiUtils._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Attribute, PartyRole, RelationshipState, TokenText}
import it.pagopa.pdnd.interop.uservice.partymanagement.service.OffsetDateTimeSupplier
import org.slf4j.LoggerFactory

import java.time.temporal.ChronoUnit
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.language.postfixOps

object PartyPersistentBehavior {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def commandHandler(
    shard: ActorRef[ClusterSharding.ShardCommand],
    context: ActorContext[Command],
    offsetDateTimeSupplier: OffsetDateTimeSupplier
  ): (State, Command) => Effect[Event, State] = { (state, command) =>
    val idleTimeout =
      context.system.settings.config.getDuration("uservice-party-management.idle-timeout")
    context.setReceiveTimeout(idleTimeout.get(ChronoUnit.SECONDS) seconds, Idle)
    command match {
      case AddParty(party, replyTo) =>
        logger.debug(s"Adding party {}", party.id)
        logger.debug(state.toString)
        state.parties
          .get(party.id)
          .map { _ =>
            logger.debug("AddParty found party {}", party.id)
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
        party.foreach(p => logger.debug("Found party {}", p.id.toString))
        replyTo ! party

        Effect.none

      case GetPartyAttributes(uuid, replyTo) =>
        val statusReply: StatusReply[Seq[Attribute]] = state.parties
          .get(uuid)
          .map {
            case institutionParty: InstitutionParty =>
              StatusReply.success(institutionParty.attributes.map(InstitutionAttribute.toApi).toSeq)
            case _: PersonParty => StatusReply.success(Seq.empty[Attribute])
          }
          .getOrElse {
            StatusReply.Error[Seq[Attribute]](s"Party $uuid not found")
          }

        replyTo ! statusReply
        Effect.none

      case GetOrganizationByExternalId(externalId, replyTo) =>
        val party: Option[InstitutionParty] = state.parties.collectFirst {
          case (_, o: InstitutionParty) if o.externalId == externalId => o
        }
        party.foreach(p => logger.debug("Found organization {}", p.id.toString))
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
          .get(partyRelationship.id)
          .map { _ =>
            replyTo ! StatusReply.Error(s"Relationship ${partyRelationship.id.toString} already exists")
            Effect.none[PartyRelationshipAdded, State]
          }
          .getOrElse {

            Effect
              .persist(PartyRelationshipAdded(partyRelationship))
              .thenRun(_ => replyTo ! StatusReply.Success(partyRelationship))

          }

      case ConfirmPartyRelationship(partyRelationshipId, filePath, fileInfo, tokenId, replyTo) =>
        state.relationships
          .get(partyRelationshipId)
          .fold {
            replyTo ! StatusReply.Error(s"Relationship ${partyRelationshipId.toString} not found")
            Effect.none[PartyRelationshipConfirmed, State]
          } { t =>
            Effect
              .persist(
                PartyRelationshipConfirmed(
                  partyRelationshipId = t.id,
                  filePath = filePath,
                  fileName = fileInfo.getFileName,
                  contentType = fileInfo.getContentType.toString(),
                  onboardingTokenId = tokenId,
                  timestamp = offsetDateTimeSupplier.get
                )
              )
              .thenRun(_ => replyTo ! StatusReply.Success(()))
          }

      case RejectPartyRelationship(partyRelationshipId, replyTo) =>
        val relationship: Option[PersistedPartyRelationship] = state.relationships.get(partyRelationshipId)

        relationship match {
          case Some(rel) =>
            Effect
              .persist(PartyRelationshipRejected(rel.id, offsetDateTimeSupplier.get))
              .thenRun(_ => replyTo ! StatusReply.Success(()))
          case None =>
            replyTo ! StatusReply.Error(s"Relationship ${partyRelationshipId.toString} not found")
            Effect.none
        }

      case DeletePartyRelationship(partyRelationshipId, replyTo) =>
        val relationship: Option[PersistedPartyRelationship] = state.relationships.get(partyRelationshipId)

        relationship match {
          case Some(rel) =>
            Effect
              .persist(PartyRelationshipDeleted(rel.id, offsetDateTimeSupplier.get))
              .thenRun(_ => replyTo ! StatusReply.Success(()))
          case None =>
            replyTo ! StatusReply.Error(s"Relationship ${partyRelationshipId.toString} not found")
            Effect.none
        }

      case SuspendPartyRelationship(partyRelationshipId, replyTo) =>
        val relationship: Option[PersistedPartyRelationship] = state.relationships.get(partyRelationshipId)

        relationship match {
          case Some(rel) =>
            Effect
              .persist(PartyRelationshipSuspended(rel.id, offsetDateTimeSupplier.get))
              .thenRun(_ => replyTo ! StatusReply.Success(()))
          case None =>
            replyTo ! StatusReply.Error(s"Relationship ${partyRelationshipId.toString} not found")
            Effect.none
        }

      case ActivatePartyRelationship(partyRelationshipId, replyTo) =>
        val relationship: Option[PersistedPartyRelationship] = state.relationships.get(partyRelationshipId)

        relationship match {
          case Some(rel) =>
            Effect
              .persist(PartyRelationshipActivated(rel.id, offsetDateTimeSupplier.get))
              .thenRun(_ => replyTo ! StatusReply.Success(()))
          case None =>
            replyTo ! StatusReply.Error(s"Relationship ${partyRelationshipId.toString} not found")
            Effect.none
        }

      case GetPartyRelationshipById(uuid, replyTo) =>
        val relationship: Option[PersistedPartyRelationship] = state.relationships.get(uuid)
        replyTo ! relationship
        Effect.none

      case GetPartyRelationshipsByFrom(from, roles, states, products, productRoles, replyTo) =>
        val relationships: List[PersistedPartyRelationship] = state.relationships.values.filter(_.from == from).toList
        val filtered: List[PersistedPartyRelationship] =
          filterRelationships(relationships, roles, states, products, productRoles)
        replyTo ! filtered
        Effect.none

      case GetPartyRelationshipsByTo(to, roles, states, products, productRoles, replyTo) =>
        val relationships: List[PersistedPartyRelationship] = state.relationships.values.filter(_.to == to).toList
        val filtered: List[PersistedPartyRelationship] =
          filterRelationships(relationships, roles, states, products, productRoles)
        replyTo ! filtered
        Effect.none

      case GetToken(tokenId, replyTo) =>
        val token: Option[Token] = state.tokens.get(tokenId)
        replyTo ! token
        Effect.none

      case AddToken(token, replyTo) =>
        val itCanBeInsert: Boolean =
          state.tokens.get(token.id).exists(t => t.isValid) || !state.tokens.contains(token.id)

        if (itCanBeInsert) {
          Effect
            .persist(TokenAdded(token))
            .thenRun(_ => replyTo ! StatusReply.Success(TokenText(token.id.toString)))
        } else {
          replyTo ! StatusReply.Error(s"Token is expired: token seed ${token.id.toString}")
          Effect.none[TokenAdded, State]
        }

      case GetPartyRelationshipByAttributes(from, to, role, product, productRole, replyTo) =>
        replyTo ! state.getPartyRelationshipByAttributes(from, to, role, product, productRole)
        Effect.none[Event, State]

      case Idle =>
        shard ! ClusterSharding.Passivate(context.self)
        Effect.none[Event, State]
    }

  }

  private def filterRelationships(
    relationships: List[PersistedPartyRelationship],
    roles: List[PartyRole],
    states: List[RelationshipState],
    products: List[String],
    productRoles: List[String]
  ): List[PersistedPartyRelationship] =
    relationships
      .filter(r => verifyParametersByCondition(roles)(r.role.toApi))
      .filter(r => verifyParametersByCondition(states)(r.state.toApi))
      .filter(r => verifyParametersByCondition(products)(r.product.id))
      .filter(r => verifyParametersByCondition(productRoles)(r.product.role))

  val eventHandler: (State, Event) => State = (state, event) =>
    event match {
      case PartyAdded(party)                         => state.addParty(party)
      case PartyDeleted(party)                       => state.deleteParty(party)
      case AttributesAdded(party)                    => state.updateParty(party)
      case PartyRelationshipAdded(partyRelationship) => state.addPartyRelationship(partyRelationship)
      case PartyRelationshipConfirmed(relationshipId, filePath, fileName, contentType, tokenId, timestamp) =>
        state.confirmPartyRelationship(relationshipId, filePath, fileName, contentType, tokenId, timestamp)
      case PartyRelationshipRejected(relationshipId, timestamp)  => state.rejectRelationship(relationshipId, timestamp)
      case PartyRelationshipDeleted(relationshipId, timestamp)   => state.deleteRelationship(relationshipId, timestamp)
      case PartyRelationshipSuspended(relationshipId, timestamp) => state.suspendRelationship(relationshipId, timestamp)
      case PartyRelationshipActivated(relationshipId, timestamp) =>
        state.activateRelationship(relationshipId, timestamp)
      case TokenAdded(token) => state.addToken(token)
    }

  val TypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("uservice-party-management-persistence-party")

  def apply(
    shard: ActorRef[ClusterSharding.ShardCommand],
    persistenceId: PersistenceId,
    offsetDateTimeSupplier: OffsetDateTimeSupplier
  ): Behavior[Command] = {
    Behaviors.setup { context =>
      val numberOfEvents =
        context.system.settings.config
          .getInt("uservice-party-management.number-of-events-before-snapshot")
      EventSourcedBehavior[Command, Event, State](
        persistenceId = persistenceId,
        emptyState = State.empty,
        commandHandler = commandHandler(shard, context, offsetDateTimeSupplier),
        eventHandler = eventHandler
      ).withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = numberOfEvents, keepNSnapshots = 1))
        .withTagger(_ => Set(persistenceId.id))
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(200 millis, 5 seconds, 0.1))
    }
  }

}
