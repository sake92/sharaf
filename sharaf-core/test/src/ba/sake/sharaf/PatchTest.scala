package ba.sake.sharaf

import ba.sake.tupson.*
import org.typelevel.jawn.ast.*

class PatchTest extends munit.FunSuite:
  import PatchTest.*

  test("missing fields default to Keep even when their underlying types have defaults"):
    assertEquals("{}".parseJson[UserPatch], UserPatch(Patch.Keep, Patch.Keep, Patch.Keep))

  test("present fields wrap their underlying values in Set"):
    val json = """{"name":"Ada","address":"London","tags":[]}"""
    assertEquals(
      json.parseJson[UserPatch],
      UserPatch(Patch.Set("Ada"), Patch.Set(Some("London")), Patch.Set(Seq.empty))
    )

  test("null clears a nullable field and leaves omitted fields unchanged"):
    assertEquals(
      """{"address":null}""".parseJson[UserPatch],
      UserPatch(Patch.Keep, Patch.Set(None), Patch.Keep)
    )

  test("invalid fields preserve underlying codec errors"):
    intercept[TupsonException]("""{"name":null}""".parseJson[UserPatch])
    intercept[TupsonException]("""{"name":42}""".parseJson[UserPatch])
    intercept[TupsonException]("""{"tags":false}""".parseJson[UserPatch])

  test("writing omits Keep and preserves explicit null and values"):
    val patch = UserPatch(Patch.Set("Ada"), Patch.Set(None), Patch.Keep)
    assertEquals(JParser.parseUnsafe(patch.toJson), JParser.parseUnsafe("""{"name":"Ada","address":null}"""))
    assertEquals(JParser.parseUnsafe(UserPatch(Patch.Keep, Patch.Keep, Patch.Keep).toJson), JObject.empty)
    assertEquals(patch.toJson.parseJson[UserPatch], patch)

  test("standalone Set uses the value codec and standalone Keep cannot be written"):
    assertEquals("42".parseJson[Patch[Int]], Patch.Set(42))
    assertEquals(JsonRW[Patch[Int]].write(Patch.Set(42)), JNum(42))
    intercept[TupsonException](JsonRW[Patch[Int]].write(Patch.Keep))

object PatchTest:
  case class UserPatch(name: Patch[String], address: Patch[Option[String]], tags: Patch[Seq[String]]) derives JsonRW
