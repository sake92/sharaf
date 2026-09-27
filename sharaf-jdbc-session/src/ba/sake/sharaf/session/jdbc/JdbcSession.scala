package ba.sake.sharaf.session.jdbc

import java.time.Instant
import ba.sake.sharaf.session.Session
import ba.sake.tupson.*

/** Mutable session whose JSON values can be persisted by [[JdbcSessionStore]].
  *
  * Session instances are request-local. The store supplies database isolation between requests.
  */
private[jdbc] final class JdbcSession(
    private var _id: String,
    val createdAt: Instant,
    private var data: Map[String, String]
) extends Session:

  private var _previousId: Option[String] = None
  private var _lastAccessedAt: Instant = createdAt
  private var _invalidated = false
  private var _regenerated = false

  override def id: String = _id

  override def previousId: Option[String] = _previousId

  override def lastAccessedAt: Instant = _lastAccessedAt

  override def keys: Set[String] = data.keySet

  override def getOpt[T: JsonRW](key: String): Option[T] =
    data.get(key).map(_.parseJson[T])

  override def set[T: JsonRW](key: String, value: T): Unit =
    data = data.updated(key, value.toJson(spaces = 0))

  override def remove(key: String): Unit =
    data = data.removed(key)

  override def touch(): Unit =
    _lastAccessedAt = Instant.now()

  override def invalidate(): Unit =
    _invalidated = true

  override def isInvalid: Boolean = _invalidated

  override def regenerate(): Unit =
    _previousId = Some(id)
    _id = SecureJdbcSessionId.generate()
    _regenerated = true

  override def isRegenerated: Boolean = _regenerated

  private[jdbc] def serializedData: String =
    data.toJson(spaces = 0)

private[jdbc] object SecureJdbcSessionId:
  private val random = new java.security.SecureRandom()

  def generate(): String =
    val bytes = new Array[Byte](16)
    random.nextBytes(bytes)
    java.util.Base64.getUrlEncoder.withoutPadding.encodeToString(bytes)
