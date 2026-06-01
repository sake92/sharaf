package ba.sake.sharaf.session

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** In-memory session store backed by a [[ConcurrentHashMap]].
  *
  * Note: sessions are lost on server restart. For production use, consider a persistent
  * store (e.g. Redis or database-backed).
  */
final class InMemorySessionStore(config: SessionConfig) extends SessionStore {

  private val store = new ConcurrentHashMap[String, Session]()

  override def create(): Session = {
    val id = SecureSessionId.generate()
    val now = Instant.now()
    val session: Session = new SessionImpl(id, now)
    store.put(id, session)
    session
  }

  override def load(sessionId: String): Option[Session] =
    Option(store.get(sessionId)).flatMap { session =>
      val now = Instant.now()
      val idleExpired = config.maxAge.exists { maxAge =>
        session.lastAccessedAt.plus(maxAge).isBefore(now)
      }
      val absoluteExpired = config.absoluteTimeout.exists { timeout =>
        session.createdAt.plus(timeout).isBefore(now)
      }
      if idleExpired || absoluteExpired then
        store.remove(sessionId)
        None
      else Some(session)
    }

  override def save(session: Session): Unit =
    store.put(session.id, session)

  override def delete(sessionId: String): Unit =
    store.remove(sessionId)

}

object InMemorySessionStore:
  def apply(config: SessionConfig = SessionConfig.default): InMemorySessionStore =
    new InMemorySessionStore(config)
