package it.pagopa.pdnd.interop.uservice.partymanagement.common

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

package object utils {
  private final val sha1: MessageDigest = MessageDigest.getInstance("SHA-1")

  def toSha1(text: String): String =
    Base64.getEncoder.encodeToString(sha1.digest(text.getBytes(StandardCharsets.UTF_8)))

}
