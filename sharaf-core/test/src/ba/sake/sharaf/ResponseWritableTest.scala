package ba.sake.sharaf

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8

import ba.sake.tupson.{JsonRW, toJson}

class ResponseWritableTest extends munit.FunSuite {

  private case class Payload(message: String, values: Seq[Int]) derives JsonRW

  test("JSON responses stream the same compact UTF-8 as the string renderer") {
    val payload = Payload("č😀\"\\\n", Seq(1, 2, 3))
    val output = new ByteArrayOutputStream()

    ResponseWritable[Payload].write(payload, output)

    val actual = new String(output.toByteArray, UTF_8)
    assertEquals(actual, payload.toJson(spaces = 0, sort = false))
  }
}
