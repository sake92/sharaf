package ba.sake.sharaf

import java.time.Instant
import java.util.Date

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
)
