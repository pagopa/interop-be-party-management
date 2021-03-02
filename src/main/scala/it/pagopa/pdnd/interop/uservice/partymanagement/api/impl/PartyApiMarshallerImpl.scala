package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.pdnd.interop.uservice.partymanagement.api.PartyApiMarshaller
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Credential, ErrorResponse, Person}
import spray.json._

import java.util.UUID
import scala.util.{Failure, Success, Try}

class PartyApiMarshallerImpl extends PartyApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def toEntityMarshallerCredential: ToEntityMarshaller[Credential] = ???

  override implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] = ???

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

  implicit object PersonFormat extends RootJsonFormat[Person] {
    override def write(person: Person): JsValue = JsObject(
      "id"         -> JsString(person.id.toString),
      "name"       -> JsString(person.name),
      "surname"    -> JsString(person.surname),
      "fiscalCode" -> JsString(person.fiscalCode)
    )

    override def read(json: JsValue): Person = {
      json.asJsObject.getFields("id", "name", "surname", "fiscalCode") match {
        case Seq(JsString(id), JsString(name), JsString(surname), JsString(fiscalCode)) =>
          Person(UUID.fromString(id), name, surname, fiscalCode)
        case _ => deserializationError("Person expected")
      }
    }

  }

  override implicit def toEntityMarshallerPerson: ToEntityMarshaller[Person] = sprayJsonMarshaller[Person]

  override implicit def fromEntityUnmarshallerPerson: FromEntityUnmarshaller[Person] = sprayJsonUnmarshaller[Person]
}
