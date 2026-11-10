package ba.sake.sharaf.jdkhttp

import ba.sake.sharaf.Cookie
import java.net.HttpCookie

object CookieUtils {

  def fromJdkHttp(c: HttpCookie): Cookie =
    Cookie(
      name = c.getName,
      value = c.getValue,
      path = Option(c.getPath),
      domain = Option(c.getDomain),
      maxAge = if c.getMaxAge == -1 then None else Some(c.getMaxAge.toInt),
      expires = None, // HttpCookie doesn't expose expires directly
      discard = c.getDiscard,
      secure = c.getSecure,
      httpOnly = c.isHttpOnly,
      version = c.getVersion,
      comment = Option(c.getComment),
      sameSite = false, // HttpCookie doesn't have SameSite support
      sameSiteMode = None
    )

  def toJdkHttp(c: Cookie): HttpCookie = {
    val cookie = new HttpCookie(c.name, c.value)
    c.path.foreach(cookie.setPath)
    c.domain.foreach(cookie.setDomain)
    c.maxAge.foreach(ma => cookie.setMaxAge(ma.toLong))
    cookie.setDiscard(c.discard)
    cookie.setSecure(c.secure)
    cookie.setHttpOnly(c.httpOnly)
    cookie.setVersion(c.version)
    c.comment.foreach(cookie.setComment)
    cookie
  }

  /** Formats a Cookie as a Set-Cookie header value */
  def toSetCookieHeader(c: Cookie): String =
    ba.sake.sharaf.CookieHeaders.render(c)

}
