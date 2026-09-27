package ba.sake.sharaf.session.jdbc

import java.time.Instant
import javax.sql.DataSource
import ba.sake.sharaf.session.{Session, SessionConfig, SessionStore}
import ba.sake.squery.{*, given}
import ba.sake.tupson.*

/** A [[SessionStore]] backed by a JDBC database through squery.
  *
  * Before creating the store, apply the template for your database from this artifact's `resources` directory through
  * your application's migration tool. Session values are stored as Tupson JSON, so they retain the same
  * [[ba.sake.tupson.JsonRW]] semantics as Sharaf's in-memory store. Database failures and corrupt serialized values
  * are propagated to the caller; they are never treated as missing sessions.
  */
final class JdbcSessionStore(dataSource: DataSource, config: SessionConfig = SessionConfig.default) extends SessionStore:

  private val context = SqueryContext(dataSource)

  override def create(): Session =
    val now = Instant.now()
    val session = new JdbcSession(SecureJdbcSessionId.generate(), now, Map.empty)
    save(session)
    session

  override def load(sessionId: String): Option[Session] =
    context.run {
      sql"""
        SELECT session_id, created_at, last_accessed_at, session_data
        FROM sharaf_session
        WHERE session_id = $sessionId
      """.readRowOpt[StoredSession]().flatMap { stored =>
        if isExpired(stored, Instant.now()) then
          delete(sessionId)
          None
        else
          Some(
            new JdbcSession(
              stored.session_id,
              Instant.ofEpochMilli(stored.created_at),
              stored.session_data.parseJson[Map[String, String]]
            )
          )
      }
    }

  override def save(session: Session): Unit =
    if session.isInvalid then delete(session.id)
    else
      session match
        case jdbcSession: JdbcSession => saveJdbcSession(jdbcSession)
        case _ =>
          throw IllegalArgumentException(
            "JdbcSessionStore can only save sessions it created because SessionStore does not expose raw serialized values"
          )

  override def delete(sessionId: String): Unit =
    context.run {
      sql"DELETE FROM sharaf_session WHERE session_id = $sessionId".update()
    }

  /** Deletes all sessions that exceed this store's idle or absolute expiry policy.
    *
    * Run this periodically to reclaim rows for sessions that are never loaded again. Loading an individual expired
    * session also deletes it immediately.
    */
  def deleteExpired(): Int =
    val now = Instant.now()
    (config.maxAge, config.absoluteTimeout) match
      case (Some(maxAge), Some(absoluteTimeout)) =>
        context.run {
          sql"""
            DELETE FROM sharaf_session
            WHERE last_accessed_at < ${now.minus(maxAge).toEpochMilli} OR
                  created_at < ${now.minus(absoluteTimeout).toEpochMilli}
          """.update()
        }
      case (Some(maxAge), None) =>
        context.run {
          sql"DELETE FROM sharaf_session WHERE last_accessed_at < ${now.minus(maxAge).toEpochMilli}".update()
        }
      case (None, Some(absoluteTimeout)) =>
        context.run {
          sql"DELETE FROM sharaf_session WHERE created_at < ${now.minus(absoluteTimeout).toEpochMilli}".update()
        }
      case (None, None) => 0

  private def saveJdbcSession(session: JdbcSession): Unit =
    context.runTransaction {
      if session.isRegenerated then session.previousId.foreach(deleteSession)
      val updated = sql"""
        UPDATE sharaf_session
        SET created_at = ${session.createdAt.toEpochMilli}, last_accessed_at = ${session.lastAccessedAt.toEpochMilli},
            session_data = ${session.serializedData}
        WHERE session_id = ${session.id}
      """.update()
      if updated == 0 then insertSession(session)
    }

  private def insertSession(session: JdbcSession)(using SqueryConnection): Unit =
    sql"""
      INSERT INTO sharaf_session(session_id, created_at, last_accessed_at, session_data)
      VALUES (${session.id}, ${session.createdAt.toEpochMilli}, ${session.lastAccessedAt.toEpochMilli},
              ${session.serializedData})
    """.insert()
    ()

  private def deleteSession(sessionId: String)(using SqueryConnection): Unit =
    sql"DELETE FROM sharaf_session WHERE session_id = $sessionId".update()
    ()

  private def isExpired(session: StoredSession, now: Instant): Boolean =
    config.maxAge.exists(Instant.ofEpochMilli(session.last_accessed_at).plus(_).isBefore(now)) ||
      config.absoluteTimeout.exists(Instant.ofEpochMilli(session.created_at).plus(_).isBefore(now))

private[jdbc] final case class StoredSession(
    session_id: String,
    created_at: Long,
    last_accessed_at: Long,
    session_data: String
) derives SqlReadRow

object JdbcSessionStore:
  def apply(dataSource: DataSource, config: SessionConfig = SessionConfig.default): JdbcSessionStore =
    new JdbcSessionStore(dataSource, config)
