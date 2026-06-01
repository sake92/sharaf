package ba.sake.sharaf.session

import java.time.Instant
import ba.sake.sharaf.*

/** A [[SharafHandler]] decorator that provides session management.
  *
  * On each request:
  *   1. Reads the session-ID cookie from the incoming request.
  *   2. Loads an existing session from the store, or creates a new one.
  *   3. Makes the session available via [[Session.current]] for the duration of the request.
  *   4. After the inner handler returns, persists the session and sets the session cookie.
  *
  * If [[Session.invalidate]] was called during the request the session is deleted and the
  * cookie is removed from the response.
  *
  * If [[Session.regenerate]] was called (recommended after login to prevent session
  * fixation) the old session is deleted and a new ID is issued.
  */
final class SessionHandler(
    store: SessionStore,
    config: SessionConfig,
    next: SharafHandler
) extends SharafHandler {

  override def handle(context: RequestContext): Response[?] = {
    val sessionId = context.request.cookies.find(_.name == config.cookieName).map(_.value)
    val session = sessionId
      .flatMap(id => store.load(id))
      .getOrElse(store.create())

    session.touch()

    SessionHolder.set(session)
    val res =
      try next.handle(context)
      finally SessionHolder.clear()

    if session.isInvalid then
      store.delete(session.id)
      res.removingCookie(config.cookieName)
    else
      if session.isRegenerated then session.previousId.foreach(store.delete)
      store.save(session)
      val maxAgeSeconds = config.maxAge.map(_.getSeconds.toInt)
      res.settingCookie(
        Cookie(
          name = config.cookieName,
          value = session.id, // always the session ID
          path = Some(config.cookiePath),
          maxAge = maxAgeSeconds,
          secure = config.secure,
          httpOnly = config.httpOnly,
          sameSite = true,
          sameSiteMode = Some(config.sameSite)
        )
      )
  }
}
