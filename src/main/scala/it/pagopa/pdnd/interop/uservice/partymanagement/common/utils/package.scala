package it.pagopa.pdnd.interop.uservice.partymanagement.common

package object utils {

  type ErrorOr[A] = Either[Throwable, A]

}
