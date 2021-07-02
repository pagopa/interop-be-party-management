package it.pagopa.pdnd.interop.uservice

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.classicActorSystem
import it.pagopa.pdnd.interop.uservice.partymanagement.service.UUIDSupplier
import org.scalamock.scalatest.MockFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

package object partymanagement extends MockFactory {
  val uuidSupplier: UUIDSupplier              = mock[UUIDSupplier]
  final lazy val url: String                  = "http://localhost:8088/pdnd-interop-uservice-party-management/0.0.1"
  final val authorization: Seq[Authorization] = Seq(headers.Authorization(OAuth2BearerToken("token")))

  def createOrganization(data: Source[ByteString, Any]): HttpResponse = create(data, "organizations")

  def createPerson(data: Source[ByteString, Any]): HttpResponse = create(data, "persons")

  def createRelationShip(data: Source[ByteString, Any]): HttpResponse = create(data, "relationships")

  def createToken(data: Source[ByteString, Any]): HttpResponse = create(data, "tokens")

  private def create(data: Source[ByteString, Any], path: String): HttpResponse = {
    Await.result(
      Http().singleRequest(
        HttpRequest(
          uri = s"$url/$path",
          method = HttpMethods.POST,
          entity = HttpEntity(ContentTypes.`application/json`, data),
          headers = authorization
        )
      ),
      Duration.Inf
    )
  }

}
