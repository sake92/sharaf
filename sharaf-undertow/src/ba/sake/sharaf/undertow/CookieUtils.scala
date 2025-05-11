package ba.sake.sharaf.undertow

import ba.sake.sharaf.Cookie
import io.undertow.server.handlers.{Cookie as UndertowCookie, CookieImpl as UndertowCookieImpl}

object CookieUtils {

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

  def toUndertow(c: Cookie): UndertowCookie = {
    import java.util.Date
    val cookie = new UndertowCookieImpl(c.name, c.value)
    c.path.foreach(cookie.setPath)
    c.domain.foreach(cookie.setDomain)
    c.maxAge.foreach(ma => cookie.setMaxAge(ma))
    c.expires.foreach(e => cookie.setExpires(Date.from(e)))
    cookie.setDiscard(c.discard)
    cookie.setSecure(c.secure)
    cookie.setHttpOnly(c.httpOnly)
    cookie.setVersion(c.version)
    c.comment.foreach(cookie.setComment)
    cookie.setSameSite(c.sameSite)
    c.sameSiteMode.foreach(cookie.setSameSiteMode)
    cookie
  }
  
  
}
