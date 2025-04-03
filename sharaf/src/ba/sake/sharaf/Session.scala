package ba.sake.sharaf

import java.time.Instant
import scala.jdk.CollectionConverters.*
import io.undertow.server.session.Session as UndertowSession
import io.undertow.util.Sessions as UndertowSessions
import ba.sake.sharaf.exceptions.SharafException

final class Session private (
    private val underlyingSession: UndertowSession
) {
  def id: String =
    underlyingSession.getId

  def createdAt: Instant =
    Instant.ofEpochMilli(underlyingSession.getCreationTime)

  def lastAccessedAt: Instant =
    Instant.ofEpochMilli(underlyingSession.getLastAccessedTime)

  def keys: Set[String] =
    underlyingSession.getAttributeNames.asScala.toSet

  def get[T <: Serializable](key: String): T =
    getOpt(key).getOrElse(throw new SharafException(s"No value found for session key: ${key}"))

  def getOpt[T <: Serializable](key: String): Option[T] =
    Option(underlyingSession.getAttribute(key)).map(_.asInstanceOf[T])

  def set[T <: Serializable](key: String, value: T): Unit =
    underlyingSession.setAttribute(key, value)

  def remove[T <: Serializable](key: String): Unit =
    underlyingSession.removeAttribute(key)

}

object Session {
  def current(using r: Request): Session =
    val undertowSession = UndertowSessions.getOrCreateSession(r.underlyingHttpServerExchange)
    Session(undertowSession)
}
