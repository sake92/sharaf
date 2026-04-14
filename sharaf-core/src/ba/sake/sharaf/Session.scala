package ba.sake.sharaf

import java.time.Instant
import ba.sake.tupson.JsonRW
import ba.sake.sharaf.exceptions.SharafException

trait Session {

  def id: String

  def createdAt: Instant

  def lastAccessedAt: Instant

  def keys: Set[String]

  def get[T: JsonRW](key: String): T =
    getOpt[T](key).getOrElse(throw new SharafException(s"No value found for session key: ${key}"))

  def getOpt[T: JsonRW](key: String): Option[T]

  def set[T: JsonRW](key: String, value: T): Unit

  def remove(key: String): Unit

  /** Destroys this session. The session cookie will be cleared from the response. */
  def invalidate(): Unit

  /** Generates a new session ID while preserving data. Call this after a user logs in
    * to prevent session fixation attacks.
    */
  def regenerate(): Unit

}

object Session:
  def current: Session =
    SessionHolder.get.getOrElse(
      throw SharafException(
        "No active session. Configure sessions with SharafHandler.sessions()."
      )
    )
