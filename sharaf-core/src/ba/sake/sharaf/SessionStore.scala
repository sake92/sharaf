package ba.sake.sharaf

/** Defines how sessions are stored and retrieved.
  *
  * Built-in implementations:
  *   - [[InMemorySessionStore]]: stores sessions in server memory (default)
  *   - [[CookieSessionStore]]: stores signed session data in the cookie itself (JVM only)
  *
  * Custom implementations can be provided for e.g. Redis or database-backed sessions.
  */
trait SessionStore {

  /** Creates a new empty session with a freshly generated ID. */
  def create(): SharafSession

  /** Loads a session by the raw cookie value.
    * Returns `None` if the session does not exist or has expired.
    *
    * For [[InMemorySessionStore]] the cookie value is the session ID.
    * For [[CookieSessionStore]] the cookie value contains the full signed session data.
    */
  def load(cookieValue: String): Option[SharafSession]

  /** Persists a session after request processing. */
  def save(session: SharafSession): Unit

  /** Removes a session (e.g. when [[Session.invalidate]] is called). */
  def delete(sessionId: String): Unit

  /** Returns the value to place in the session cookie.
    *
    * For [[InMemorySessionStore]] this is `session.id`.
    * For [[CookieSessionStore]] this is the signed+encoded session data.
    */
  def cookieValue(session: SharafSession): String = session.id
}
