package it.pagopa.pdnd.interop.uservice.partymanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.{HttpResponse, MessageEntity}
import it.pagopa.pdnd.interop.uservice.partymanagement.model._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object PersonPartyApiServiceData {

  // format: off
  final lazy val personUuid1 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9215"
  final lazy val personUuid2 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9216"
  final lazy val personUuid3 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9217"
  final lazy val personUuid4 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9218"
  final lazy val personUuid5 = "27f8dce0-0a5b-476b-9fdd-a7a658eb9281"

  final lazy val taxCode1 = "RSSMRA75L01H501A"
  final lazy val taxCode2 = "RSSMRA75L01H501B"
  final lazy val taxCode3 = "RSSMRA75L01H501C"
  final lazy val taxCode4 = "RSSMRA75L01H501D"

  final lazy val personSeed1 = PersonSeed(taxCode = taxCode1, surname = "Doe", name = "Joe")
  final lazy val personSeed2 = PersonSeed(taxCode = taxCode2, surname = "Durden", name = "Tyler")
  final lazy val personSeed3 = PersonSeed(taxCode = taxCode3, surname = "Soze", name = "Keiser")
  final lazy val personSeed4 = PersonSeed(taxCode = taxCode4, surname = "Plissken", name = "Snake")

  final lazy val personExpected1 = Person(taxCode = taxCode1, surname = "Doe", name = "Joe", partyId = personUuid1)
  final lazy val personExpected2 = Person(taxCode = taxCode3, surname = "Soze", name = "Keiser", partyId = personUuid3)

  // format: on

  def prepareTest(
    personSeed: PersonSeed
  )(implicit as: ActorSystem, mp: Marshaller[PersonSeed, MessageEntity], ec: ExecutionContext): HttpResponse = {
    val personRequestData = Await.result(Marshal(personSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)

    createPerson(personRequestData)
  }
}
