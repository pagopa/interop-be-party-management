package it.pagopa.interop.partymanagement.model.party

sealed trait QueueEvent

object QueueEvent {
  case object ADD    extends QueueEvent
  case object UPDATE extends QueueEvent
}
