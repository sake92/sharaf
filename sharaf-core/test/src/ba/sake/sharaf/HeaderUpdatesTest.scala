package ba.sake.sharaf

class HeaderUpdatesTest extends munit.FunSuite {
  test("resolution keeps repeated values in order and preserves removals of engine headers") {
    val bodyHeaders = Seq(HttpString("Content-Type") -> Seq("text/plain"), HttpString("X-Values") -> Seq("old"))
    val updates = HeaderUpdates(Seq.empty)
      .setting(HttpString("x-values"), Seq("first", "second"))
      .setting(HttpString("content-type"), Seq.empty)
      .removing(HttpString("X-Engine"))
      .setting(HttpString("X-Restored"), "old")
      .removing(HttpString("x-restored"))
      .setting(HttpString("X-RESTORED"), "final")
    assertEquals(
      updates.resolve(bodyHeaders).toSet,
      Set(
        HeaderUpdate.Set(HttpString("X-Values"), Seq("first", "second")),
        HeaderUpdate.Remove(HttpString("Content-Type")),
        HeaderUpdate.Remove(HttpString("X-Engine")),
        HeaderUpdate.Set(HttpString("X-Restored"), Seq("final"))
      )
    )
  }
}
