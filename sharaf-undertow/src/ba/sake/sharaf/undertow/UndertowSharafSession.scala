package ba.sake.sharaf.undertow

import java.time.Instant
import scala.jdk.CollectionConverters.*

final class UndertowSharafSession(
    private val underlyingSession: io.undertow.server.session.Session
) extends ba.sake.sharaf.Session {

  override def id: String =
    underlyingSession.getId

  override def createdAt: Instant =
    Instant.ofEpochMilli(underlyingSession.getCreationTime)

  override def lastAccessedAt: Instant =
    Instant.ofEpochMilli(underlyingSession.getLastAccessedTime)

  override def keys: Set[String] =
    underlyingSession.getAttributeNames.asScala.toSet

  override def getOpt[T <: Serializable](key: String): Option[T] =
    Option(underlyingSession.getAttribute(key)).map(_.asInstanceOf[T])

  override def set[T <: Serializable](key: String, value: T): Unit =
    underlyingSession.setAttribute(key, value)

  override def remove[T <: Serializable](key: String): Unit =
    underlyingSession.removeAttribute(key)

}
