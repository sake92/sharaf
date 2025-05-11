package ba.sake.sharaf

import java.time.Instant
import ba.sake.sharaf.exceptions.SharafException

trait Session {
  
  def id: String

  def createdAt: Instant

  def lastAccessedAt: Instant

  def keys: Set[String]

  def get[T <: Serializable](key: String): T =
    getOpt(key).getOrElse(throw new SharafException(s"No value found for session key: ${key}"))

  def getOpt[T <: Serializable](key: String): Option[T]

  def set[T <: Serializable](key: String, value: T): Unit
  
  def remove[T <: Serializable](key: String): Unit 

}

object Session:
  def current(using s: Session):  Session = s
