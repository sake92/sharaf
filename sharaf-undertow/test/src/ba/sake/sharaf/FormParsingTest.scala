package ba.sake.sharaf

import scala.collection.immutable.SeqMap
import io.undertow.server.handlers.form.FormData as UFormData
import ba.sake.formson.FormValue

class FormParsingTest extends munit.FunSuite {

  test("Preserve insertion order") {
    val uFormData = UFormData(50)
    for i <- 0 until 50 do uFormData.add(s"a$i", "bla")

    val formsonMap = UndertowSharafRequest.undertowFormData2FormsonMap(uFormData)

    assertEquals(
      formsonMap,
      SeqMap.from(
        for i <- 0 until 50 yield singleValue(s"a$i", "bla")
      )
    )
  }

  private def singleValue(k: String, value: String): (String, Seq[FormValue]) =
    k -> Seq(FormValue.Str(value))
}
