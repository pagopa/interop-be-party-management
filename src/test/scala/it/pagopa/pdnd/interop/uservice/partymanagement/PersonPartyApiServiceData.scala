package it.pagopa.pdnd.interop.uservice.partymanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.{HttpResponse, MessageEntity}
import it.pagopa.pdnd.interop.uservice.partymanagement.model._

import java.util.UUID
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object PersonPartyApiServiceData {

  final lazy val personUuid1 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9215")
  final lazy val personUuid2 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9216")
  final lazy val personUuid3 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9217")
  final lazy val personUuid4 = UUID.fromString("27f8dce0-0a5b-476b-9fdd-a7a658eb9218")

  final lazy val personSeed1 = PersonSeed(id = personUuid1)
  final lazy val personSeed2 = PersonSeed(id = personUuid2)
  final lazy val personSeed3 = PersonSeed(id = personUuid3)

  final lazy val personExpected1 = Person(id = personUuid1)
  final lazy val personExpected2 = Person(id = personUuid3)

  def prepareTest(
    personSeed: PersonSeed
  )(implicit as: ActorSystem, mp: Marshaller[PersonSeed, MessageEntity], ec: ExecutionContext): HttpResponse = {
    val personRequestData = Await.result(Marshal(personSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    createPerson(personRequestData)
  }
}
