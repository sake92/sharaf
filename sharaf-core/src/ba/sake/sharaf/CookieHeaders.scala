package ba.sake.sharaf

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Wire format shared by adapters without their own cookie support. */
private[sharaf] object CookieHeaders {
  def parse(headers: Seq[String]): Seq[Cookie] =
    headers.flatMap { header =>
      header.split(';').toSeq.flatMap { item =>
        val parts = item.trim.split("=", 2)
        val name = parts(0).trim
        val value = if parts.length == 2 then parts(1).trim else ""
        Option.when(name.nonEmpty)(Cookie(name, value))
      }
    }

  def render(cookie: Cookie): String = {
    val result = new StringBuilder(s"${cookie.name}=${cookie.value}")
    cookie.path.foreach(value => result.append(s"; Path=$value"))
    cookie.domain.foreach(value => result.append(s"; Domain=$value"))
    cookie.maxAge.foreach(value => result.append(s"; Max-Age=$value"))
    cookie.expires.foreach { value =>
      val expires = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC).format(value)
      result.append(s"; Expires=$expires")
    }
    if cookie.secure then result.append("; Secure")
    if cookie.httpOnly then result.append("; HttpOnly")
    cookie.sameSiteMode.foreach(value => result.append(s"; SameSite=$value"))
    result.toString
  }
}
