package ba.sake.sharaf

import java.time.Instant

/** Cookie updates represented as a series of immutable transformations. This is handy when you dynamically remove
  * header(s), maybe set by a previous Undertow handler.
  *
  * @param updates
  *   Series of cookie transformations
  */
private[sharaf] final case class CookieUpdates(updates: Seq[Cookie]) {

  def setting(value: Cookie): CookieUpdates =
    copy(updates = updates.appended(value))

  def removing(name: String): CookieUpdates =
    // this is the best you can do to remove a cookie...
    val removingCookie = Cookie(name, "", expires = Some(Instant.EPOCH))
    setting(removingCookie)

}
