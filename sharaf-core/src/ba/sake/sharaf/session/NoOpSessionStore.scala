package ba.sake.sharaf.session

import java.time.Instant

/** A [[SessionStore]] that creates ephemeral sessions but never persists them.
  *
  * Useful for stateless authentication (e.g. JWT) where a session object is needed for
  * the duration of a single request but no server-side storage or session cookie is
  * required.
  *
  * `load` always returns [[None]], so a fresh session is created on every request.
  * `save` and `delete` are no-ops.
  */
final class NoOpSessionStore extends SessionStore {

  override def create(): Session =
    new SessionImpl(SecureSessionId.generate(), Instant.now())

  override def load(sessionId: String): Option[Session] = None

  override def save(session: Session): Unit = ()

  override def delete(sessionId: String): Unit = ()
}

object NoOpSessionStore:
  val instance: NoOpSessionStore = new NoOpSessionStore
