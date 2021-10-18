package it.pagopa.pdnd.interop.uservice.partymanagement

import java.util.UUID
import scala.util.Try

package object service {

  type OnboardingFilePath = String

  /** Pimps string type to support safe UUID parsing.
    * <br>
    * @param string to be extended
    */
  //TODO make it as an extension method in Scala 3
  implicit class ExtendedString(val str: String) extends AnyVal {
    def asUUID: Either[Throwable, UUID] = Try { UUID.fromString(str) }.toEither
  }
}
