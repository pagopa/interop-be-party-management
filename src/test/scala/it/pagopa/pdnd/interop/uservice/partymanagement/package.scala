package it.pagopa.pdnd.interop.uservice

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import it.pagopa.pdnd.interop.commons.utils.service.UUIDSupplier
import it.pagopa.pdnd.interop.uservice.partymanagement.api.impl._
import it.pagopa.pdnd.interop.uservice.partymanagement.model._
import it.pagopa.pdnd.interop.uservice.partymanagement.service.OffsetDateTimeSupplier
import org.scalamock.scalatest.MockFactory

import java.io.{File, PrintWriter}
import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Random

package object partymanagement extends MockFactory {
  val uuidSupplier: UUIDSupplier                     = mock[UUIDSupplier]
  val offsetDateTimeSupplier: OffsetDateTimeSupplier = mock[OffsetDateTimeSupplier]

  final val timestamp = OffsetDateTime.parse("2021-11-26T13:57:43.314689+01:00")

  final lazy val url: String =
    s"http://localhost:8088/pdnd-interop-uservice-party-management/${buildinfo.BuildInfo.interfaceVersion}"
  final val authorization: Seq[Authorization] = Seq(headers.Authorization(OAuth2BearerToken("token")))
  final val multipart: Seq[HttpHeader] = Seq(
    headers.Authorization(OAuth2BearerToken("token")),
    headers.`Content-Type`(ContentType(MediaTypes.`multipart/form-data`))
  )

  def createOrganization(data: Source[ByteString, Any])(implicit actorSystem: ActorSystem): HttpResponse =
    create(data, "organizations")

  def createPerson(data: Source[ByteString, Any])(implicit actorSystem: ActorSystem): HttpResponse =
    create(data, "persons")

  def createRelationship(data: Source[ByteString, Any])(implicit actorSystem: ActorSystem): HttpResponse =
    create(data, "relationships")

  def createToken(data: Source[ByteString, Any])(implicit actorSystem: ActorSystem): HttpResponse =
    create(data, "tokens")

  def confirmRelationshipWithToken(
    relationship: Relationship
  )(implicit as: ActorSystem, ec: ExecutionContext): HttpResponse = {
    val tokenSeed =
      TokenSeed(
        id = UUID.randomUUID().toString,
        relationships = Relationships(Seq(relationship)),
        "checksum",
        OnboardingContractInfo("test", "test")
      )
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

  def writeToTempFile(contents: String): File = {
    val temporaryFile = File.createTempFile("temp", System.currentTimeMillis().toString)
    temporaryFile.deleteOnExit()
    new PrintWriter(temporaryFile) {
      try {
        write(contents)
      } finally {
        close()
      }
    }
    temporaryFile
  }

  def randomString(): String = Random.alphanumeric.take(40).mkString
}
