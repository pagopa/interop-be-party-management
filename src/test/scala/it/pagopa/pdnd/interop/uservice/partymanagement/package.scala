package it.pagopa.pdnd.interop.uservice

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import it.pagopa.pdnd.interop.uservice.partymanagement.api.impl._
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.service.UUIDSupplier
import org.scalamock.scalatest.MockFactory

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

package object partymanagement extends MockFactory {
  val uuidSupplier: UUIDSupplier = mock[UUIDSupplier]
  final lazy val url: String =
    s"http://localhost:8088/pdnd-interop-uservice-party-management/${buildinfo.BuildInfo.interfaceVersion}"
  final val authorization: Seq[Authorization] = Seq(headers.Authorization(OAuth2BearerToken("token")))

  def createOrganization(data: Source[ByteString, Any])(implicit actorSystem: ActorSystem): HttpResponse =
    create(data, "organizations")

  def createPerson(data: Source[ByteString, Any])(implicit actorSystem: ActorSystem): HttpResponse =
    create(data, "persons")

  def createRelationship(data: Source[ByteString, Any])(implicit actorSystem: ActorSystem): HttpResponse =
    create(data, "relationships")

  def createToken(data: Source[ByteString, Any])(implicit actorSystem: ActorSystem): HttpResponse =
    create(data, "tokens")

  def confirmRelationshipWithToken(
    relationshipSeed: RelationshipSeed
  )(implicit as: ActorSystem, ec: ExecutionContext): HttpResponse = {
    val tokenSeed =
      TokenSeed(seed = UUID.randomUUID().toString, relationships = RelationshipsSeed(Seq(relationshipSeed)), "checksum")
    val tokenData = Await.result(Marshal(tokenSeed).to[MessageEntity].map(_.dataBytes), Duration.Inf)
    createToken(tokenData)
  }

  private def create(data: Source[ByteString, Any], path: String)(implicit actorSystem: ActorSystem): HttpResponse = {
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
