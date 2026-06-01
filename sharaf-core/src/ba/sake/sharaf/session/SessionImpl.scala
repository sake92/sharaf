package ba.sake.sharaf.session

import java.time.Instant
import ba.sake.tupson.{*, given}

/** Mutable session implementation backed by a JSON-serialized key-value map. */
final class SessionImpl(
    private var _id: String,
    val createdAt: Instant
) extends Session {

  private var _previousId: Option[String] = None
  private var _lastAccessedAt: Instant = createdAt
  private var _data: Map[String, String] = Map.empty
  private var _invalidated: Boolean = false
  private var _regenerated: Boolean = false

  override def id: String = _id

  override def previousId: Option[String] = _previousId

  override def lastAccessedAt: Instant = _lastAccessedAt

  override def keys: Set[String] = _data.keySet

  override def getOpt[T: JsonRW](key: String): Option[T] =
    _data.get(key).map(_.parseJson[T])

  override def set[T: JsonRW](key: String, value: T): Unit =
    _data = _data + (key -> value.toJson)

  override def remove(key: String): Unit =
    _data = _data - key

  override def touch(): Unit =
    _lastAccessedAt = Instant.now()

  override def invalidate(): Unit =
    _invalidated = true

  override def isInvalid: Boolean = _invalidated

  override def regenerate(): Unit =
    _previousId = Some(id)
    _id = SecureSessionId.generate()
    _regenerated = true

  override def isRegenerated: Boolean = _regenerated
}
 