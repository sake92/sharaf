package ba.sake.sharaf.session.jdbc

import java.time.Duration
import java.util.UUID
import org.h2.jdbcx.JdbcDataSource
import ba.sake.sharaf.session.SessionConfig
import ba.sake.squery.*
import ba.sake.tupson.JsonRW

class JdbcSessionStoreTest extends munit.FunSuite:

  private final case class Profile(name: String, roles: Seq[String]) derives JsonRW

  test("persists typed values across store instances after the application migrates the schema") {
    val dataSource = newDataSource()
    createSchema(dataSource)
    val store = JdbcSessionStore(dataSource)

    val session = store.create()
    session.set("profile", Profile("Ada", Seq("admin")))
    session.set("attempts", 2)
    store.save(session)

    val restored = JdbcSessionStore(dataSource).load(session.id).getOrElse(fail("session was not persisted"))
    assertEquals(restored.getOpt[Profile]("profile"), Some(Profile("Ada", Seq("admin"))))
    assertEquals(restored.getOpt[Int]("attempts"), Some(2))
  }

  test("regeneration replaces the old ID and preserves session values atomically") {
    val store = newStore()
    val session = store.create()
    session.set("user", "ada")
    store.save(session)
    val oldId = session.id

    session.regenerate()
    store.save(session)

    assertEquals(store.load(oldId), None)
    val renewed = store.load(session.id).getOrElse(fail("renewed session was not saved"))
    assertEquals(renewed.getOpt[String]("user"), Some("ada"))
  }

  test("loading an idle-expired session deletes it") {
    val config = SessionConfig.default
      .withMaxAge(Some(Duration.ZERO))
      .withAbsoluteTimeout(None)
    val store = newStore(config)
    val session = store.create()

    assertEquals(store.load(session.id), None)
    assertEquals(store.load(session.id), None)
  }

  test("periodic cleanup removes absolute-expired sessions") {
    val config = SessionConfig.default
      .withMaxAge(None)
      .withAbsoluteTimeout(Some(Duration.ZERO))
    val store = newStore(config)
    val session = store.create()

    assertEquals(store.deleteExpired(), 1)
    assertEquals(store.load(session.id), None)
  }

  test("saving an invalidated session removes it") {
    val store = newStore()
    val session = store.create()
    session.invalidate()

    store.save(session)

    assertEquals(store.load(session.id), None)
  }

  private def newStore(config: SessionConfig = SessionConfig.default): JdbcSessionStore =
    val dataSource = newDataSource()
    createSchema(dataSource)
    JdbcSessionStore(dataSource, config)

  private def createSchema(dataSource: JdbcDataSource): Unit =
    SqueryContext(dataSource).run {
      sql"""
        CREATE TABLE sharaf_session (
          session_id VARCHAR(128) PRIMARY KEY,
          created_at BIGINT NOT NULL,
          last_accessed_at BIGINT NOT NULL,
          session_data TEXT NOT NULL
        )
      """.update()
    }

  private def newDataSource(): JdbcDataSource =
    val dataSource = new JdbcDataSource()
    dataSource.setURL(s"jdbc:h2:mem:sharaf_session_${UUID.randomUUID().toString};DB_CLOSE_DELAY=-1")
    dataSource
