package ba.sake.sharaf

import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import ba.sake.tupson.{*, given}

/** Client-side session store that serialises the entire session into a signed cookie.
  *
  * No server-side storage is required. Each response carries the full session state in
  * a cookie that is verified with HMAC-SHA256 to prevent tampering.
  *
  * The cookie value has the format: `<base64url(json)>.<base64url(hmac)>`
  *
  * @param secretKey
  *   Secret key used for HMAC-SHA256 signing. Must be kept confidential.
  * @param config
  *   Session configuration (timeouts, cookie flags, etc.)
  */
final class CookieSessionStore(secretKey: String, config: SessionConfig) extends SessionStore {

  private val separator = "."

  override def create(): SharafSession = {
    val id = SecureSessionId.generate()
    val now = Instant.now()
    new SharafSession(id, now, now, Map.empty)
  }

  override def load(cookieValue: String): Option[SharafSession] =
    parseCookieValue(cookieValue).flatMap { session =>
      val now = Instant.now()
      val idleExpired = config.maxAge.exists { maxAge =>
        session._lastAccessedAt.plus(maxAge).isBefore(now)
      }
      val absoluteExpired = config.absoluteTimeout.exists { timeout =>
        session._createdAt.plus(timeout).isBefore(now)
      }
      if idleExpired || absoluteExpired then None
      else Some(session)
    }

  override def save(session: SharafSession): Unit = () // data lives in the cookie

  override def delete(sessionId: String): Unit = () // handled by SessionHandler removing the cookie

  override def cookieValue(session: SharafSession): String =
    val payload = CookieSessionStore.SessionData(
      session.id,
      session._createdAt.toEpochMilli,
      session._lastAccessedAt.toEpochMilli,
      session._data
    )
    val json = payload.toJson
    val encoded = Base64.getUrlEncoder.withoutPadding.encodeToString(json.getBytes("UTF-8"))
    val sig = hmacSha256(encoded)
    s"$encoded$separator$sig"

  private def parseCookieValue(raw: String): Option[SharafSession] =
    val separatorIdx = raw.lastIndexOf(separator)
    if separatorIdx < 0 then None
    else
      val encoded = raw.substring(0, separatorIdx)
      val sig = raw.substring(separatorIdx + separator.length)
      val expectedSig = hmacSha256(encoded)
      if !constantTimeEquals(sig, expectedSig) then None
      else
        try
          val json = new String(Base64.getUrlDecoder.decode(encoded), "UTF-8")
          val payload = json.parseJson[CookieSessionStore.SessionData]
          Some(
            new SharafSession(
              payload.id,
              Instant.ofEpochMilli(payload.createdAtMillis),
              Instant.ofEpochMilli(payload.lastAccessedAtMillis),
              payload.data
            )
          )
        catch case _: Exception => None

  private def hmacSha256(data: String): String = {
    val mac = Mac.getInstance("HmacSHA256")
    val keySpec = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256")
    mac.init(keySpec)
    val hmacBytes = mac.doFinal(data.getBytes("UTF-8"))
    Base64.getUrlEncoder.withoutPadding.encodeToString(hmacBytes)
  }

  /** Constant-time string comparison to prevent timing attacks. */
  private def constantTimeEquals(a: String, b: String): Boolean = {
    if a.length != b.length then return false
    var result = 0
    for i <- a.indices do result |= a(i) ^ b(i)
    result == 0
  }
}

object CookieSessionStore:
  def apply(secretKey: String, config: SessionConfig = SessionConfig.default): CookieSessionStore =
    new CookieSessionStore(secretKey, config)

  private case class SessionData(
      id: String,
      createdAtMillis: Long,
      lastAccessedAtMillis: Long,
      data: Map[String, String]
  ) derives JsonRW
