package ba.sake.sharaf

import java.security.SecureRandom
import java.util.Base64

private[sharaf] object SecureSessionId {

  private val rng = new SecureRandom()

  def generate(): String = {
    val bytes = new Array[Byte](16) // 128-bit random ID
    rng.nextBytes(bytes)
    Base64.getUrlEncoder.withoutPadding.encodeToString(bytes)
  }
}
