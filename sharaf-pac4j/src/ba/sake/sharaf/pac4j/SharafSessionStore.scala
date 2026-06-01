package ba.sake.sharaf.pac4j

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util.{Base64, Optional}
import scala.util.Using
import org.pac4j.core.context.{FrameworkParameters, WebContext}
import org.pac4j.core.context.session.{SessionStore as Pac4jSessionStore, SessionStoreFactory}
import ba.sake.sharaf.session.{SessionStore as SharafSessionStoreTrait, SessionImpl, SessionHolder}

/** Adapts a Sharaf [[SharafSessionStoreTrait]] to pac4j's [[Pac4jSessionStore]].
  *
  * Bridges pac4j's key-value `Object` storage to Sharaf's typed JSON session via Java
  * serialization → Base64 encoding. pac4j values are stored under `"pac4j."` prefixed keys
  * in the Sharaf session.
  */
final class SharafSessionStore(store: SharafSessionStoreTrait) extends Pac4jSessionStore:

  private val prefix = "pac4j."

  override def getSessionId(context: WebContext, createSession: Boolean): Optional[String] =
    val currentSession = SessionHolder.get
    currentSession match
      case Some(s) => Optional.of(s.id)
      case None if createSession =>
        val newSession = store.create()
        SessionHolder.set(newSession)
        Optional.of(newSession.id)
      case _ => Optional.empty()

  override def get(context: WebContext, key: String): Optional[AnyRef] =
    SessionHolder.get match
      case Some(session) =>
        session.getOpt[String](s"$prefix$key") match
          case Some(encoded) => Optional.ofNullable(deserialize(encoded))
          case None          => Optional.empty()
      case None => Optional.empty()

  override def set(context: WebContext, key: String, value: AnyRef): Unit =
    SessionHolder.get.foreach { session =>
      val encoded = serialize(value)
      session.set(s"$prefix$key", encoded)
    }

  override def destroySession(context: WebContext): Boolean =
    SessionHolder.get match
      case Some(session) =>
        store.delete(session.id)
        session.invalidate()
        true
      case None => false

  override def renewSession(context: WebContext): Boolean =
    SessionHolder.get match
      case Some(session) =>
        val oldId = session.id
        session.regenerate()
        store.delete(oldId)
        store.save(session.asInstanceOf[SessionImpl])
        true
      case None => false

  override def getTrackableSession(context: WebContext): Optional[AnyRef] =
    SessionHolder.get match
      case Some(session) => Optional.of(session.id)
      case None          => Optional.empty()

  override def buildFromTrackableSession(
      context: WebContext,
      trackableSession: AnyRef
  ): Optional[Pac4jSessionStore] =
    Optional.empty() // not supported for in-memory stores

  // --- serialization helpers ---

  private def serialize(obj: AnyRef): String =
    Using(new ByteArrayOutputStream()) { baos =>
      Using(new ObjectOutputStream(baos)) { oos =>
        oos.writeObject(obj)
      }
      Base64.getEncoder.encodeToString(baos.toByteArray)
    }.get

  private def deserialize(encoded: String): AnyRef =
    val bytes = Base64.getDecoder.decode(encoded)
    Using(new ByteArrayInputStream(bytes)) { bais =>
      Using(new ObjectInputStream(bais)) { ois =>
        ois.readObject()
      }.get
    }.get

object SharafSessionStore:
  /** Creates a pac4j [[SessionStoreFactory]] that wraps a Sharaf [[SharafSessionStoreTrait]]. */
  def factory(store: SharafSessionStoreTrait): SessionStoreFactory =
    (_: FrameworkParameters) => new SharafSessionStore(store)
