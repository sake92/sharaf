package ba.sake.sharaf

import java.time.Instant
import ba.sake.tupson.{*, given}

/** Mutable session implementation backed by a JSON-serialized key-value map. */
final class SharafSession(
    private[sharaf] var _id: String,
    private[sharaf] val _createdAt: Instant,
    private[sharaf] var _lastAccessedAt: Instant,
    private[sharaf] var _data: Map[String, String]
) extends Session {

  private[sharaf] var _invalidated: Boolean = false
  private[sharaf] var _regenerated: Boolean = false
  private[sharaf] var _previousId: Option[String] = None

  override def id: String = _id

  override def createdAt: Instant = _createdAt

  override def lastAccessedAt: Instant = _lastAccessedAt

  override def keys: Set[String] = _data.keySet

  override def getOpt[T: JsonRW](key: String): Option[T] =
    _data.get(key).map(_.parseJson[T])

  override def set[T: JsonRW](key: String, value: T): Unit =
    _data = _data + (key -> value.toJson)

  override def remove(key: String): Unit =
    _data = _data - key

  override def invalidate(): Unit =
    _invalidated = true

  override def regenerate(): Unit =
    _previousId = Some(_id)
    _id = SecureSessionId.generate()
    _regenerated = true

}
