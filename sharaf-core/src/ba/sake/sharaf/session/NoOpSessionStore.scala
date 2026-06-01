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
final class NoOpSessionStore extends SessionStore:

  override def create(): SessionImpl =
    new SessionImpl(SecureSessionId.generate(), Instant.now(), Instant.now(), Map.empty)

  override def load(sessionId: String): Option[SessionImpl] = None

  override def save(session: SessionImpl): Unit = ()

  override def delete(sessionId: String): Unit = ()

object NoOpSessionStore:
  val instance: NoOpSessionStore = new NoOpSessionStore
