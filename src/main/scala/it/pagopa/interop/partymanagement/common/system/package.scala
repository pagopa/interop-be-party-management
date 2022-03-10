package it.pagopa.interop.partymanagement.common

import akka.util.Timeout
import it.pagopa.interop.partymanagement.model.{Institution, Person}

import scala.concurrent.duration.DurationInt

package object system {

  type ApiParty = Either[Institution, Person]

  implicit val timeout: Timeout = 3.seconds

}
