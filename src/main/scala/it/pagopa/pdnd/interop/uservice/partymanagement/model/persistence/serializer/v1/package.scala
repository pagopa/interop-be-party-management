package it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer

import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.{formatter, toOffsetDateTime}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.party._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.party.PartyV1.Empty
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.party.{
  InstitutionPartyV1,
  PartyV1,
  PersonPartyV1
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.relationship.{
  PartyRelationShipIdV1,
  PartyRelationShipStatusV1,
  PartyRelationShipV1,
  PartyRoleV1
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.state.{
  IndexesV1,
  PartiesV1,
  RelationShipsV1,
  StateV1,
  TokensV1
}
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.serializer.v1.token.{TokenStatusV1, TokenV1}
import cats.implicits._
import it.pagopa.pdnd.interop.uservice.partymanagement.model.persistence.State

import java.util.UUID

package object v1 {

  // Party Ops
  implicit def partyV1ProtobufDeserializer: ProtobufDeserializer[PartyV1, Party] = {
    case p: PersonPartyV1 =>
      Right(
        PersonParty(
          id = UUID.fromString(p.id),
          externalId = p.externalId,
          name = p.name,
          surname = p.surname,
          start = toOffsetDateTime(p.start),
          end = p.end.map(toOffsetDateTime)
        )
      )
    case i: InstitutionPartyV1 =>
      Right(
        InstitutionParty(
          id = UUID.fromString(i.id),
          externalId = i.externalId,
          description = i.description,
          digitalAddress = i.digitalAddress,
          manager = i.manager,
          start = toOffsetDateTime(i.start),
          end = i.end.map(toOffsetDateTime)
        )
      )
    case Empty => Left(new RuntimeException("Deserialization from protobuf failed"))
  }

  implicit def partyProtobufSerializer: ProtobufSerializer[Party, PartyV1] = {
    case p: PersonParty =>
      Right(
        PersonPartyV1(
          id = p.id.toString,
          externalId = p.externalId,
          name = p.name,
          surname = p.surname,
          start = p.start.format(formatter),
          end = p.end.map(_.format(formatter))
        )
      )
    case i: InstitutionParty =>
      Right(
        InstitutionPartyV1(
          id = i.id.toString,
          externalId = i.externalId,
          description = i.description,
          digitalAddress = i.digitalAddress,
          manager = i.manager,
          start = i.start.format(formatter),
          end = i.end.map(_.format(formatter))
        )
      )
  }

  // PartyRole* Ops
  implicit def partyRoleV1ProtobufDeserializer: ProtobufDeserializer[PartyRoleV1, PartyRole] = partyRoleV1 =>
    PartyRole.fromText(partyRoleV1.name)

  implicit def partyRoleV1ProtobufSerializer: ProtobufSerializer[PartyRole, PartyRoleV1] =
    partyRole =>
      PartyRoleV1.fromName(partyRole.stringify).toRight(new RuntimeException("Deserialization from protobuf failed"))
  // PartyRelationShipStatus* Ops
  implicit def partyRelationShipStatusV1ProtobufDeserializer
    : ProtobufDeserializer[PartyRelationShipStatusV1, PartyRelationShipStatus] = partyRelationShipStatusV1 =>
    PartyRelationShipStatus.fromText(partyRelationShipStatusV1.name)

  implicit def partyRelationShipStatusV1ProtobufSerializer
    : ProtobufSerializer[PartyRelationShipStatus, PartyRelationShipStatusV1] =
    partyRelationShipStatus =>
      PartyRelationShipStatusV1
        .fromName(partyRelationShipStatus.stringify)
        .toRight(new RuntimeException("Deserialization from protobuf failed"))

  // PartyRelationShipId* Ops
  implicit def partyRelationShipIdV1ProtobufDeserializer
    : ProtobufDeserializer[PartyRelationShipIdV1, PartyRelationShipId] =
    partyRelationShipIdV1 =>
      PartyRole
        .fromText(partyRelationShipIdV1.status.name)
        .map(role =>
          PartyRelationShipId(
            UUID.fromString(partyRelationShipIdV1.from),
            UUID.fromString(partyRelationShipIdV1.to),
            role
          )
        )

  implicit def partyRelationShipIdV1ProtobufSerializer: ProtobufSerializer[PartyRelationShipId, PartyRelationShipIdV1] =
    partyRelationShipId =>
      ProtobufSerializer
        .to(partyRelationShipId.role)
        .map(role => PartyRelationShipIdV1(partyRelationShipId.from.toString, partyRelationShipId.to.toString, role))

  // PartyRelationShip* Ops
  implicit def partyRelationShipV1ProtobufDeserializer: ProtobufDeserializer[PartyRelationShipV1, PartyRelationShip] =
    partyRelationShipV1 =>
      for {
        id     <- ProtobufDeserializer.from(partyRelationShipV1.id)
        status <- ProtobufDeserializer.from(partyRelationShipV1.status)
      } yield PartyRelationShip(
        id = id,
        start = toOffsetDateTime(partyRelationShipV1.start),
        end = partyRelationShipV1.end.map(toOffsetDateTime),
        status = status
      )

  implicit def partyRelationShipV1ProtobufSerializer: ProtobufSerializer[PartyRelationShip, PartyRelationShipV1] =
    partyRelationShip =>
      for {
        id     <- ProtobufSerializer.to(partyRelationShip.id)
        status <- ProtobufSerializer.to(partyRelationShip.status)
      } yield PartyRelationShipV1(
        id = id,
        start = partyRelationShip.start.format(formatter),
        end = partyRelationShip.end.map(_.format(formatter)),
        status = status
      )

  // Token* Ops
  implicit def tokenStatusV1ProtobufSerializer: ProtobufSerializer[TokenStatus, TokenStatusV1] =
    tokenStatus =>
      TokenStatusV1
        .fromName(tokenStatus.stringify)
        .toRight(new RuntimeException("Deserialization from protobuf failed"))

  implicit def tokenStatusV1ProtobufDeserializer: ProtobufDeserializer[TokenStatusV1, TokenStatus] = tokenStatusV1 =>
    TokenStatus.fromText(tokenStatusV1.name)

  implicit def tokenV1ProtobufDeserializer: ProtobufDeserializer[TokenV1, Token] =
    tokenV1 =>
      for {
        manager  <- ProtobufDeserializer.from(tokenV1.manager)
        delegate <- ProtobufDeserializer.from(tokenV1.manager)
        status   <- ProtobufDeserializer.from(tokenV1.status)
      } yield Token(
        manager = manager,
        delegate = delegate,
        validity = toOffsetDateTime(tokenV1.validity),
        status = status,
        seed = UUID.fromString(tokenV1.seed)
      )

  implicit def tokenV1ProtobufSerializer: ProtobufSerializer[Token, TokenV1] =
    token =>
      for {
        manager  <- ProtobufSerializer.to(token.manager)
        delegate <- ProtobufSerializer.to(token.delegate)
        status   <- ProtobufSerializer.to(token.status)
      } yield TokenV1(
        manager = manager,
        delegate = delegate,
        validity = token.validity.format(formatter),
        status = status,
        seed = token.seed.toString
      )

  // State
  implicit def partiesV1ProtobufDeserializer: ProtobufDeserializer[Seq[PartiesV1], Map[UUID, Party]] =
    partiesV1 =>
      partiesV1
        .traverse(p => ProtobufDeserializer.from(p.value).map(party => UUID.fromString(p.key) -> party))
        .map(_.toMap)

  implicit def partiesV1ProtobufSerializer: ProtobufSerializer[Map[UUID, Party], Seq[PartiesV1]] =
    parties =>
      parties.toSeq.traverse { case (k, v) => ProtobufSerializer.to(v).map(party => PartiesV1(k.toString, party)) }

  implicit def indexesV1ProtobufDeserializer: ProtobufDeserializer[Seq[IndexesV1], Map[String, UUID]] =
    indexesV1 => Right(indexesV1.map(p => p.key -> UUID.fromString(p.value)).toMap)

  implicit def indexesV1ProtobufSerializer: ProtobufSerializer[Map[String, UUID], Seq[IndexesV1]] =
    indexes => Right(indexes.map { case (k, v) => IndexesV1(k, v.toString) }.toSeq)

  implicit def tokensV1ProtobufDeserializer: ProtobufDeserializer[Seq[TokensV1], Map[UUID, Token]] =
    tokensV1 =>
      tokensV1
        .traverse(p => ProtobufDeserializer.from(p.value).map(token => UUID.fromString(p.key) -> token))
        .map(_.toMap)

  implicit def tokensV1ProtobufSerializer: ProtobufSerializer[Map[UUID, Token], Seq[TokensV1]] =
    tokens =>
      tokens.toSeq.traverse { case (k, v) => ProtobufSerializer.to(v).map(token => TokensV1(k.toString, token)) }

  implicit def relationShipsV1ProtobufDeserializer
    : ProtobufDeserializer[Seq[RelationShipsV1], Map[PartyRelationShipId, PartyRelationShip]] =
    relationShipsV1 =>
      relationShipsV1
        .traverse { p =>
          for {
            partyRelationShipId <- ProtobufDeserializer.from(p.key)
            partyRelationShip   <- ProtobufDeserializer.from(p.value)
          } yield partyRelationShipId -> partyRelationShip
        }
        .map(_.toMap)

  implicit def relationShipsV1ProtobufSerializer
    : ProtobufSerializer[Map[PartyRelationShipId, PartyRelationShip], Seq[RelationShipsV1]] =
    relationShips =>
      relationShips.toSeq.traverse { p =>
        for {
          partyRelationShipId <- ProtobufSerializer.to(p._1)
          partyRelationShip   <- ProtobufSerializer.to(p._2)
        } yield RelationShipsV1(partyRelationShipId, partyRelationShip)
      }

  implicit def stateV1ProtobufDeserializer: ProtobufDeserializer[StateV1, State] =
    stateV1 =>
      for {
        parties       <- ProtobufDeserializer.from(stateV1.parties)
        indexes       <- ProtobufDeserializer.from(stateV1.indexes)
        tokens        <- ProtobufDeserializer.from(stateV1.tokens)
        relationShips <- ProtobufDeserializer.from(stateV1.relationShips)
      } yield State(parties, indexes, tokens, relationShips)

  implicit def stateV1ProtobufSerializer: ProtobufSerializer[State, StateV1] =
    state =>
      for {
        parties       <- ProtobufSerializer.to(state.parties)
        indexes       <- ProtobufSerializer.to(state.indexes)
        tokens        <- ProtobufSerializer.to(state.tokens)
        relationShips <- ProtobufSerializer.to(state.relationShips)
      } yield StateV1(parties, indexes, tokens, relationShips)

}
