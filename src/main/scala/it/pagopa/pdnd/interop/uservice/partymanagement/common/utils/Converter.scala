package it.pagopa.pdnd.interop.uservice.partymanagement.common.utils

trait Converter[A] {
  type B
  def value(a: A): B
}

object Converter {

  def convert[A](a: A)(implicit c: Converter[A]): c.B = c.value(a)

  type Aux[A, B0] = Converter[A] { type B = B0 }

}
