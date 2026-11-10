package ba.sake.sharaf.handlers

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import sttp.client4.quick.*
import sttp.model.StatusCode
import ba.sake.sharaf.*

abstract class AbstractResponseBodyTest extends munit.FunSuite with ServerFixture {

  for kind <- Seq("inputstream", "geny") do {
    test(s"$kind response writes its bytes and content type") {
      val text = "response bytes čćž"
      val routes = Routes { case GET -> Path() =>
        if kind == "inputstream" then Response.withBody(new ByteArrayInputStream(text.getBytes(UTF_8)))
        else {
          val writable: geny.Writable = text.getBytes(UTF_8)
          Response.withBody(writable)
        }
      }
      withServer(routes, ExceptionMapper.default) { base =>
        val response = request.get(uri"$base/").send(httpBackend)
        assertEquals(response.code, StatusCode.Ok)
        assertEquals(response.body, text)
        assertEquals(response.headers("Content-Type"), Seq("application/octet-stream"))
      }
    }
  }

  test("file response writes its bytes, content type and filename") {
    val directory = Files.createTempDirectory("sharaf-response-")
    val file = directory.resolve("text_file.txt")
    try {
      Files.write(file, "a text file".getBytes(UTF_8))
      val routes = Routes { case GET -> Path() => Response.withBody(file) }
      withServer(routes, ExceptionMapper.default) { base =>
        val response = request.get(uri"$base/").send(httpBackend)
        assertEquals(response.code, StatusCode.Ok)
        assertEquals(response.body, "a text file")
        assertEquals(response.headers("Content-Type"), Seq("application/octet-stream"))
        assertEquals(response.headers("Content-Disposition"), Seq("attachment; filename=\"text_file.txt\""))
      }
    } finally {
      Files.deleteIfExists(file)
      Files.delete(directory)
    }
  }
}
