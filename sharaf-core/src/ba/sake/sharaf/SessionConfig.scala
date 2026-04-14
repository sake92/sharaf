package ba.sake.sharaf

import java.time.Duration

/** Configuration for session handling. */
final class SessionConfig private (
    val cookieName: String,
    val cookiePath: String,
    val maxAge: Option[Duration],
    val absoluteTimeout: Option[Duration],
    val secure: Boolean,
    val httpOnly: Boolean,
    val sameSite: String
) {

  def withCookieName(cookieName: String): SessionConfig =
    copy(cookieName = cookieName)

  def withCookiePath(cookiePath: String): SessionConfig =
    copy(cookiePath = cookiePath)

  def withMaxAge(maxAge: Option[Duration]): SessionConfig =
    copy(maxAge = maxAge)

  def withAbsoluteTimeout(absoluteTimeout: Option[Duration]): SessionConfig =
    copy(absoluteTimeout = absoluteTimeout)

  def withSecure(secure: Boolean): SessionConfig =
    copy(secure = secure)

  def withHttpOnly(httpOnly: Boolean): SessionConfig =
    copy(httpOnly = httpOnly)

  def withSameSite(sameSite: String): SessionConfig =
    copy(sameSite = sameSite)

  private def copy(
      cookieName: String = cookieName,
      cookiePath: String = cookiePath,
      maxAge: Option[Duration] = maxAge,
      absoluteTimeout: Option[Duration] = absoluteTimeout,
      secure: Boolean = secure,
      httpOnly: Boolean = httpOnly,
      sameSite: String = sameSite
  ) = new SessionConfig(cookieName, cookiePath, maxAge, absoluteTimeout, secure, httpOnly, sameSite)

  override def toString: String =
    s"SessionConfig(cookieName=$cookieName, cookiePath=$cookiePath, maxAge=$maxAge, " +
      s"absoluteTimeout=$absoluteTimeout, secure=$secure, httpOnly=$httpOnly, sameSite=$sameSite)"
}

object SessionConfig:
  val default: SessionConfig = new SessionConfig(
    cookieName = "SHARAF_SESSION",
    cookiePath = "/",
    maxAge = Some(Duration.ofMinutes(30)),
    absoluteTimeout = Some(Duration.ofHours(8)),
    secure = true,
    httpOnly = true,
    sameSite = "Strict"
  )
