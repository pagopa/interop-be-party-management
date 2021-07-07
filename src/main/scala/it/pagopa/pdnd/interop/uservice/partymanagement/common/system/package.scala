package it.pagopa.pdnd.interop.uservice.partymanagement.common

import akka.http.scaladsl.server.Directives.Authenticator
import akka.http.scaladsl.server.directives.Credentials
import akka.util.Timeout
import it.pagopa.pdnd.interop.uservice.partymanagement.model.{Organization, Person}

import scala.concurrent.duration.DurationInt

package object system {

  type ApiParty = Either[Organization, Person]

  implicit val timeout: Timeout = 3.seconds

//  object Authenticator extends Authenticator[Seq[(String, String)]] {
//
//    override def apply(credentials: Credentials): Option[Seq[(String, String)]] = {
//      credentials match {
//        case Provided(identifier) => Some(Seq("bearer" -> identifier))
//        case Missing              => None
//      }
//    }
//
//  }
  object Authenticator extends Authenticator[Seq[(String, String)]] {

    override def apply(credentials: Credentials): Option[Seq[(String, String)]] = Some(Seq.empty)

  }
}
