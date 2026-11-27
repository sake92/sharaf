package ba.sake.sharaf

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets.UTF_8

class BodyBytesTest extends munit.FunSuite {

  /** Returns at most `chunk` bytes per read, like a network stream. */
  private final class ChunkedStream(bytes: Array[Byte], chunk: Int) extends InputStream {
    private var position = 0
    override def read(): Int =
      if position >= bytes.length then -1
      else { position += 1; bytes(position - 1) & 0xff }
    override def read(buffer: Array[Byte], offset: Int, length: Int): Int =
      if length == 0 then 0
      else if position >= bytes.length then -1
      else {
        val count = math.min(math.min(length, chunk), bytes.length - position)
        System.arraycopy(bytes, position, buffer, offset, count)
        position += count
        count
      }
  }

  private def bytes(size: Int): Array[Byte] = Array.tabulate(size)(i => (i * 31 + 7).toByte)

  private def assertReads(body: Array[Byte], declared: Option[Long], chunk: Int = 8192)(using munit.Location): Unit =
    assert(
      BodyBytes.read(new ChunkedStream(body, chunk), declared).sameElements(body),
      s"declared=$declared size=${body.length}"
    )

  test("reads empty bodies with or without a declared length") {
    assertReads(Array.emptyByteArray, Some(0))
    assertReads(Array.emptyByteArray, None)
  }

  test("reads bodies that match the declared length") {
    for size <- Seq(1, 3, 1023, 1024, 1025, 16384, BodyBytes.MaxPreallocatedBytes) do
      assertReads(bytes(size), Some(size.toLong))
  }

  test("reads bodies longer than the preallocation limit") {
    val body = bytes(3 * 1024 * 1024 + 17)
    assertReads(body, Some(body.length.toLong))
    assertReads(body, Some(body.length.toLong), chunk = 7)
  }

  test("reads undeclared bodies of any size") {
    for size <- Seq(1, 1024, 1025, 100_000, 3 * 1024 * 1024 + 17) do assertReads(bytes(size), None, chunk = 1000)
  }

  test("reads to end of stream when the declared length is too small") {
    assertReads(bytes(5000), Some(10))
    assertReads(bytes(5000), Some(0))
  }

  test("returns the bytes received when the declared length is too large") {
    assertReads(bytes(10), Some(5000))
    assertReads(bytes(10), Some(Long.MaxValue))
  }

  test("reads byte by byte") {
    assertReads(bytes(3000), Some(3000), chunk = 1)
    assertReads(bytes(3000), None, chunk = 1)
  }

  test("uses only a single valid Content-Length value") {
    assertEquals(BodyBytes.declaredLength(Some(Seq("42"))), Some(42L))
    assertEquals(BodyBytes.declaredLength(Some(Seq("0"))), Some(0L))
    assertEquals(BodyBytes.declaredLength(Some(Seq("999999999999999999"))), Some(999999999999999999L))
    assertEquals(BodyBytes.declaredLength(None), None)
    assertEquals(BodyBytes.declaredLength(Some(Seq.empty)), None)
    assertEquals(BodyBytes.declaredLength(Some(Seq(""))), None)
    assertEquals(BodyBytes.declaredLength(Some(Seq("4", "4"))), None)
    assertEquals(BodyBytes.declaredLength(Some(Seq("4, 4"))), None)
    assertEquals(BodyBytes.declaredLength(Some(Seq("-1"))), None)
    assertEquals(BodyBytes.declaredLength(Some(Seq("1e3"))), None)
    assertEquals(BodyBytes.declaredLength(Some(Seq("9223372036854775808"))), None)
  }

  test("Request.bodyString decodes UTF-8 using the declared length") {
    val body = "Hello čćž".getBytes(UTF_8)
    val req = new Request {
      def headers: Map[HttpString, Seq[String]] =
        Map(HttpString("content-length") -> Seq(body.length.toString), HttpString("Content-Type") -> Seq("text/plain"))
      def queryParamsRaw: ba.sake.querson.QueryStringMap = Map.empty
      override def bodyInputStream: InputStream = new ByteArrayInputStream(body)
    }
    assertEquals(req.bodyString, "Hello čćž")
  }
}
