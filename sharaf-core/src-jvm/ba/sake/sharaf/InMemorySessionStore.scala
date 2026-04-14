package ba.sake.sharaf

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*

/** In-memory session store backed by a [[ConcurrentHashMap]] for thread-safe JVM use.
  *
  * Note: sessions are lost on server restart. For production use, consider a persistent
  * store (e.g. Redis or database-backed).
  */
final class InMemorySessionStore(config: SessionConfig) extends SessionStore {

  private val store = new ConcurrentHashMap[String, SharafSession]()

  override def create(): SharafSession = {
    val id = SecureSessionId.generate()
    val now = Instant.now()
    val session = new SharafSession(id, now, now, Map.empty)
    store.put(id, session)
    session
  }

  override def load(cookieValue: String): Option[SharafSession] =
    Option(store.get(cookieValue)).flatMap { session =>
      val now = Instant.now()
      val idleExpired = config.maxAge.exists { maxAge =>
        session._lastAccessedAt.plus(maxAge).isBefore(now)
      }
      val absoluteExpired = config.absoluteTimeout.exists { timeout =>
        session._createdAt.plus(timeout).isBefore(now)
      }
      if idleExpired || absoluteExpired then
        store.remove(cookieValue)
        None
      else Some(session)
    }

  override def save(session: SharafSession): Unit =
    store.put(session.id, session)

  override def delete(sessionId: String): Unit =
    store.remove(sessionId)

}

object InMemorySessionStore:
  def apply(config: SessionConfig = SessionConfig.default): InMemorySessionStore =
    new InMemorySessionStore(config)
