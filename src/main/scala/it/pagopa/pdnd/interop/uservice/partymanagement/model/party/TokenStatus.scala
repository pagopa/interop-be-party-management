package it.pagopa.pdnd.interop.uservice.partymanagement.model.party

import spray.json.{JsString, JsValue, JsonFormat, deserializationError}

import scala.util.{Failure, Success, Try}

sealed trait TokenStatus {
  def stringify: String = this match {
    case Waiting  => "Waiting"
    case Invalid  => "Invalid"
    case Consumed => "Consumed"
  }
}

case object Waiting  extends TokenStatus
case object Invalid  extends TokenStatus
case object Consumed extends TokenStatus

@SuppressWarnings(Array("org.wartremover.warts.Nothing"))
object TokenStatus {

  def fromText(str: String): Either[Throwable, TokenStatus] = str match {
    case "Waiting"  => Right(Waiting)
    case "Invalid"  => Right(Invalid)
    case "Consumed" => Right(Consumed)
    case _          => Left(new RuntimeException("Deserialization from protobuf failed"))
  }

  implicit val format: JsonFormat[TokenStatus] = new JsonFormat[TokenStatus] {
    override def write(obj: TokenStatus): JsValue = obj match {
      case Waiting  => JsString("Waiting")
      case Invalid  => JsString("Invalid")
      case Consumed => JsString("Consumed")
    }

    override def read(json: JsValue): TokenStatus = json match {
      case JsString(s) =>
        val res: Try[TokenStatus] = s match {
          case "Waiting"  => Success(Waiting)
          case "Invalid"  => Success(Invalid)
          case "Consumed" => Success(Consumed)
          case _          => Failure(new RuntimeException("Invalid token status"))
        }
        res.fold(ex => deserializationError(msg = ex.getMessage, cause = ex), identity)
      case notAJsString =>
        deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
    }

  }
}
