package ba.sake.sharaf

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import ba.sake.sharaf.exceptions.BadRequestException

/** Wire format shared by adapters without their own cookie support. */
private[sharaf] object CookieHeaders {
  val defaultMaxCookies: Int = 200

  def parse(headers: Seq[String], maxCookies: Int = defaultMaxCookies): Seq[Cookie] = {
    val cookies = Seq.newBuilder[Cookie]
    var count = 0
    headers.foreach { header =>
      var start = 0
      while start <= header.length do {
        val separator = header.indexOf(';', start)
        val end = if separator < 0 then header.length else separator
        val item = header.substring(start, end).trim
        val equals = item.indexOf('=')
        val name = (if equals < 0 then item else item.substring(0, equals)).trim
        if name.nonEmpty then {
          count += 1
          if count > maxCookies then throw BadRequestException("Cookie count limit exceeded")
          val value = if equals < 0 then "" else item.substring(equals + 1).trim
          cookies += Cookie(name, value)
        }
        start = if separator < 0 then header.length + 1 else separator + 1
      }
    }
    cookies.result()
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
