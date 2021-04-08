package it.pagopa.pdnd.interop.uservice

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source
import akka.util.ByteString
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.classicActorSystem
import it.pagopa.pdnd.interop.uservice.partymanagement.service.UUIDSupplier
import org.scalamock.scalatest.MockFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

package object partymanagement extends MockFactory {
  val uuidSupplier: UUIDSupplier = mock[UUIDSupplier]
  final lazy val url: String     = "http://localhost:18088/pdnd-interop-uservice-party-management/0.0.1"

  def createOrganization(data: Source[ByteString, Any]): HttpResponse = create(data, "organizations")

  def createPerson(data: Source[ByteString, Any]): HttpResponse = create(data, "persons")

  def createRelationShip(data: Source[ByteString, Any]): HttpResponse = create(data, "relationships")

  private def create(data: Source[ByteString, Any], path: String): HttpResponse = {
    Await.result(
      Http().singleRequest(
        HttpRequest(
          uri = s"$url/$path",
          method = HttpMethods.POST,
          entity = HttpEntity(ContentTypes.`application/json`, data)
        )
      ),
      Duration.Inf
    )
  }
}
