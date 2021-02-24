package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import java.time.Instant

sealed trait Party {
  def id: String
  def `type`: PartyType
  def start: Instant
  def end: Option[Instant]
  def status: PartyStatus
}

final case class Person(
  id: String,
  name: String,
  surname: String,
  email: String,
  fiscalCode: String,
  `type`: PartyType,
  start: Instant,
  end: Option[Instant],
  status: PartyStatus,
  credential: Option[Credential]
) extends Party

final case class Organization(
  id: String,
  ipaCod: String,
  name: String,
  manager: String,
  fiscalCode: String,
  `type`: PartyType,
  start: Instant,
  end: Option[Instant],
  status: PartyStatus
) extends Party
