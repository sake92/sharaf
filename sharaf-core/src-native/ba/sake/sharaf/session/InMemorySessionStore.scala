package ba.sake.sharaf.session

import java.time.Instant
import scala.collection.mutable

/** In-memory session store backed by a [[mutable.HashMap]].
  *
  * Suitable for SNUnit (single-threaded per worker). For multi-threaded environments
  * use the JVM variant which uses a [[java.util.concurrent.ConcurrentHashMap]].
  *
  * Note: sessions are lost on server restart.
  */
final class InMemorySessionStore(config: SessionConfig) extends SessionStore {

  private val store = mutable.HashMap.empty[String, SessionImpl]

  override def create(): SessionImpl = {
    val id = SecureSessionId.generate()
    val now = Instant.now()
    val session = new SessionImpl(id, now, now, Map.empty)
    store.put(id, session)
    session
  }

  override def load(sessionId: String): Option[SessionImpl] =
    store.get(sessionId).flatMap { session =>
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
