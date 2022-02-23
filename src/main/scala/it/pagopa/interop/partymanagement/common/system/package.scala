package it.pagopa.interop.partymanagement.common

import akka.util.Timeout
import it.pagopa.interop.partymanagement.model.{Organization, Person}

import scala.concurrent.duration.DurationInt

package object system {

  type ApiParty = Either[Organization, Person]

  implicit val timeout: Timeout = 3.seconds

}
