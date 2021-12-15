package it.pagopa.pdnd.interop.uservice.partymanagement.common

import akka.util.Timeout
import com.typesafe.scalalogging.CanLog
import it.pagopa.pdnd.interop.commons.utils.CorrelationId
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Organization, Person}

import scala.concurrent.duration.DurationInt

package object system {

  type ApiParty = Either[Organization, Person]

  implicit val timeout: Timeout = 3.seconds

  implicit case object CanLogCorrelationId extends CanLog[CorrelationId] {
    override def logMessage(originalMsg: String, a: CorrelationId): String = s"${a.value} $originalMsg"
  }
}
