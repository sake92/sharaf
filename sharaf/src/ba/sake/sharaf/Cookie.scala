package ba.sake.sharaf

import java.time.Instant
import java.util.Date
import io.undertow.server.handlers.Cookie as UndertowCookie
import io.undertow.server.handlers.CookieImpl as UndertowCookieImpl

final case class Cookie(
    name: String,
    value: String,
    path: Option[String] = None,
    domain: Option[String] = None,
    maxAge: Option[Int] = None,
    expires: Option[Instant] = None,
    discard: Boolean = false,
    secure: Boolean = false,
    httpOnly: Boolean = false,
    version: Int = 0,
    comment: Option[String] = None,
    sameSite: Boolean = false,
    sameSiteMode: Option[String] = None
) {
  def toUndertow: UndertowCookie = {
    val cookie = new UndertowCookieImpl(name, value)
    path.foreach(cookie.setPath)
    domain.foreach(cookie.setDomain)
    maxAge.foreach(ma => cookie.setMaxAge(ma))
    expires.foreach(e => cookie.setExpires(Date.from(e)))
    cookie.setDiscard(discard)
    cookie.setSecure(secure)
    cookie.setHttpOnly(httpOnly)
    cookie.setVersion(version)
    comment.foreach(cookie.setComment)
    cookie.setSameSite(sameSite)
    sameSiteMode.foreach(cookie.setSameSiteMode)
    cookie
  }
}

object Cookie {
  def fromUndertow(c: UndertowCookie): Cookie =
    Cookie(
      name = c.getName,
      value = c.getValue,
      path = Option(c.getPath),
      domain = Option(c.getDomain),
      maxAge = Option(c.getMaxAge).map(_.toInt),
      expires = Option(c.getExpires).map(_.toInstant),
      discard = c.isDiscard,
      secure = c.isSecure,
      httpOnly = c.isHttpOnly,
      version = c.getVersion,
      comment = Option(c.getComment),
      sameSite = c.isSameSite,
      sameSiteMode = Option(c.getSameSiteMode)
    )
}
