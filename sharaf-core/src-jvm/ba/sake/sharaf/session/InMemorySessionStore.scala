package ba.sake.sharaf.session

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** In-memory session store backed by a [[ConcurrentHashMap]] for thread-safe JVM use.
  *
  * Note: sessions are lost on server restart. For production use, consider a persistent
  * store (e.g. Redis or database-backed).
  */
final class InMemorySessionStore(config: SessionConfig) extends SessionStore {

  private val store = new ConcurrentHashMap[String, SessionImpl]()

  override def create(): SessionImpl = {
    val id = SecureSessionId.generate()
    val now = Instant.now()
    val session = new SessionImpl(id, now, now, Map.empty)
    store.put(id, session)
    session
  }

  override def load(sessionId: String): Option[SessionImpl] =
    Option(store.get(sessionId)).flatMap { session =>
      val now = Instant.now()
      val idleExpired = config.maxAge.exists { maxAge =>
        session._lastAccessedAt.plus(maxAge).isBefore(now)
      }
      val absoluteExpired = config.absoluteTimeout.exists { timeout =>
        session._createdAt.plus(timeout).isBefore(now)
      }
      if idleExpired || absoluteExpired then
        store.remove(sessionId)
        None
      else Some(session)
    }

  override def save(session: SessionImpl): Unit =
    store.put(session.id, session)

  override def delete(sessionId: String): Unit =
    store.remove(sessionId)

}

object InMemorySessionStore:
  def apply(config: SessionConfig = SessionConfig.default): InMemorySessionStore =
    new InMemorySessionStore(config)
