package ba.sake.sharaf.session

class NoOpSessionStoreTest extends munit.FunSuite:

  val store = NoOpSessionStore.instance

  test("create returns a fresh SessionImpl with unique id"):
    val session = store.create()
    assert(session.id.nonEmpty)
    assert(session.keys.isEmpty)

  test("load always returns None"):
    val session = store.create()
    assertEquals(store.load(session.id), None)
    assertEquals(store.load("nonexistent"), None)

  test("save is a no-op"):
    val session = store.create()
    store.save(session)  // should not throw

  test("delete is a no-op"):
    val session = store.create()
    store.delete(session.id)  // should not throw

  test("each create produces a unique session"):
    val s1 = store.create()
    val s2 = store.create()
    assertNotEquals(s1.id, s2.id)

  test("session is usable with typed get/set"):
    val session = store.create()
    session.set("key1", "value1")
    assertEquals(session.getOpt[String]("key1"), Some("value1"))
