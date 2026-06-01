package ba.sake.sharaf.session

/** Defines how sessions are stored and retrieved.
  *
  * Built-in implementations:
  *   - [[InMemorySessionStore]]: stores sessions in server memory (default)
  *   - [[NoOpSessionStore]]: ephemeral sessions for stateless auth (no persistence)
  *
  * Custom implementations can be provided for e.g. Redis or database-backed sessions.
  */
trait SessionStore {

  /** Creates a new empty session with a freshly generated ID. */
  def create(): Session

  /** Loads a session by its ID. Returns `None` if the session does not exist or has expired. */
  def load(sessionId: String): Option[Session]

  /** Persists a session after request processing. */
  def save(session: Session): Unit

  /** Removes a session (e.g. when [[Session.invalidate]] is called). */
  def delete(sessionId: String): Unit
}
