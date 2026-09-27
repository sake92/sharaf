package ba.sake.sharaf.session.jdbc

import org.sqlite.SQLiteDataSource
import ba.sake.squery.*

class JdbcSessionStoreNativeTest extends munit.FunSuite:

  test("persists a session with the Scala Native SQLite JDBC driver") {
    val databaseFile = new java.io.File(s"/tmp/sharaf-jdbc-session-${System.nanoTime()}.sqlite")
    try {
      val dataSource = SQLiteDataSource()
      dataSource.setUrl(s"jdbc:sqlite:${databaseFile.getPath}")
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
      val store = JdbcSessionStore(dataSource)
      val session = store.create()
      session.set("user", "ada")
      store.save(session)

      val restored = store.load(session.id).getOrElse(fail("session was not persisted"))
      assertEquals(restored.getOpt[String]("user"), Some("ada"))
    } finally databaseFile.delete()
  }
