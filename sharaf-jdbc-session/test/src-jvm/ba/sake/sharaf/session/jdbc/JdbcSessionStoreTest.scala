package ba.sake.sharaf.session.jdbc

import java.time.Duration
import java.nio.charset.StandardCharsets
import java.util.UUID
import org.h2.jdbcx.JdbcDataSource
import ba.sake.sharaf.session.SessionConfig
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

  test("a stale ordinary save does not recreate a deleted session") {
    val dataSource = newDataSource()
    createSchema(dataSource)
    val store = JdbcSessionStore(dataSource)
    val session = store.create()
    val staleSession = store.load(session.id).getOrElse(fail("session was not loaded"))

    store.delete(session.id)
    staleSession.set("user", "ada")
    store.save(staleSession)

    assertEquals(store.load(session.id), None)
  }

  test("a regenerated session cannot recreate itself after its first save") {
    val store = newStore()
    val session = store.create()
    session.regenerate()
    store.save(session)

    store.delete(session.id)
    store.save(session)

    assertEquals(store.load(session.id), None)
  }

  test("invalidating a regenerated unsaved session deletes its previous ID") {
    val store = newStore()
    val session = store.create()
    val previousId = session.id
    session.regenerate()
    session.invalidate()

    store.save(session)

    assertEquals(store.load(previousId), None)
  }

  private def newStore(config: SessionConfig = SessionConfig.default): JdbcSessionStore =
    val dataSource = newDataSource()
    createSchema(dataSource)
    JdbcSessionStore(dataSource, config)

  private def createSchema(dataSource: JdbcDataSource): Unit =
    val script =
      val input = Option(getClass.getResourceAsStream("/ba/sake/sharaf/session/jdbc/schema-h2.sql"))
        .getOrElse(fail("bundled H2 schema template was not found"))
      try new String(input.readAllBytes(), StandardCharsets.UTF_8)
      finally input.close()
    val connection = dataSource.getConnection
    try
      val statement = connection.createStatement()
      try script.split(';').map(_.trim).filter(_.nonEmpty).foreach(statement.execute)
      finally statement.close()
    finally connection.close()

  private def newDataSource(): JdbcDataSource =
    val dataSource = new JdbcDataSource()
    dataSource.setURL(s"jdbc:h2:mem:sharaf_session_${UUID.randomUUID().toString};DB_CLOSE_DELAY=-1")
    dataSource
