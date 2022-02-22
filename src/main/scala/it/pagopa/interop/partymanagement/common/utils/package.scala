package it.pagopa.interop.partymanagement.common

package object utils {

  type ErrorOr[A] = Either[Throwable, A]

}
