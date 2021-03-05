package it.pagopa.pdnd.interop.uservice.partymanagement.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{ErrorResponse, Institution, PartyRelationShip, Person}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}

import java.net.URI
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.util.{Failure, Success, Try}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  final val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  implicit val uuidFormat: JsonFormat[UUID] =
    new JsonFormat[UUID] {
      override def write(obj: UUID): JsValue = JsString(obj.toString)

      override def read(json: JsValue): UUID = json match {
        case JsString(s) =>
          Try(UUID.fromString(s)) match {
            case Success(result) => result
            case Failure(exception) =>
              deserializationError(s"could not parse $s as Joda LocalDateTime", exception)
          }
        case notAJsString =>
          deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
      }
    }

  implicit val offsetDateTimeFormat: JsonFormat[OffsetDateTime] =
    new JsonFormat[OffsetDateTime] {
      override def write(obj: OffsetDateTime): JsValue = JsString(obj.format(formatter))

      override def read(json: JsValue): OffsetDateTime = json match {
        case JsString(s) =>
          Try(OffsetDateTime.parse(s, formatter)) match {
            case Success(result) => result
            case Failure(exception) =>
              deserializationError(s"could not parse $s as Joda LocalDateTime", exception)
          }
        case notAJsString =>
          deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
      }
    }

  implicit val uriFormat: JsonFormat[URI] =
    new JsonFormat[URI] {
      override def write(obj: URI): JsValue = JsString(obj.toString)

      override def read(json: JsValue): URI = json match {
        case JsString(s) =>
          Try(URI.create(s)) match {
            case Success(result) => result
            case Failure(exception) =>
              deserializationError(s"could not parse $s as Joda LocalDateTime", exception)
          }
        case notAJsString =>
          deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
      }
    }

  implicit val personFormat: RootJsonFormat[Person]                       = jsonFormat5(Person)
  implicit val institutionFormat: RootJsonFormat[Institution]             = jsonFormat6(Institution)
  implicit val partyRelationShipFormat: RootJsonFormat[PartyRelationShip] = jsonFormat3(PartyRelationShip)
  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse]         = jsonFormat3(ErrorResponse)

}
