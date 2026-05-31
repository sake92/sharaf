package ba.sake.sharaf.pac4j

import java.util.Optional as JOptional
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.{SessionStore, SessionStoreFactory}
import org.pac4j.core.context.FrameworkParameters

/** A [[SessionStore]] that does nothing — every operation is a no-op.
  *
  * Useful for stateless authentication (e.g., JWT direct clients) where no server-side session
  * is needed. Note that [[SecurityService.currentUser]] will always return `None` with this
  * store because PAC4j cannot persist profiles between the security flow and subsequent lookups.
  */
class NoopSessionStore extends SessionStore {

  override def getTrackableSession(context: WebContext): JOptional[AnyRef] = JOptional.empty()

  override def buildFromTrackableSession(context: WebContext, trackableSession: Any): JOptional[SessionStore] =
    JOptional.empty()

  override def getSessionId(context: WebContext, createSession: Boolean): JOptional[String] = JOptional.empty()

  override def get(context: WebContext, key: String): JOptional[AnyRef] = JOptional.empty()

  override def set(context: WebContext, key: String, value: Any): Unit = ()

  override def destroySession(context: WebContext): Boolean = false

  override def renewSession(context: WebContext): Boolean = false
}

object NoopSessionStore {

  /** A [[SessionStoreFactory]] that always returns the same shared [[NoopSessionStore]] instance. */
  val factory: SessionStoreFactory =
    (_: FrameworkParameters) => NoopSessionStore()

  private val instance = new NoopSessionStore

  def apply(): NoopSessionStore = instance
}
