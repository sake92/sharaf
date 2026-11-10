package ba.sake.sharaf.handlers

import java.nio.file.Files
import java.nio.charset.StandardCharsets.UTF_8
import sttp.client4.quick.*
import sttp.model.{HeaderNames, StatusCode}
import ba.sake.sharaf.*

abstract class AbstractFilesHandlerTest extends munit.FunSuite with ServerFixture {

  private def withFiles(check: String => Unit): Unit = {
    val directory = Files.createTempDirectory("sharaf-static-")
    val file = directory.resolve("text_file.txt")
    try {
      Files.write(file, "a text file".getBytes(UTF_8))
      val routesHandler = SharafHandler.routes(
        Routes { case GET -> Path("hello") => Response.withBody("Hello World!") },
        SharafHandler.files(directory)
      )
      withHandler(SharafHandler.exceptions(routesHandler))(check)
    } finally {
      Files.deleteIfExists(file)
      Files.delete(directory)
    }
  }

  test("GET text_file.txt should work") {
    withFiles { baseUrl =>
      val res = request.get(uri"$baseUrl/text_file.txt").send()
      assertEquals(res.body, "a text file")
      assertEquals(res.headers(HeaderNames.ContentType), Seq("text/plain"))
    }
  }

  test("HEAD text_file.txt should return headers only") {
    withFiles { baseUrl =>
      val res = request.head(uri"$baseUrl/text_file.txt").send()
      assertEquals(res.body, "")
      assertEquals(res.headers(HeaderNames.ContentType), Seq("text/plain"))
    }
  }

  test("POST text_file.txt should be rejected") {
    withFiles { baseUrl =>
      val res = request.post(uri"$baseUrl/text_file.txt").send()
      assertEquals(res.code, StatusCode.MethodNotAllowed)
    }
  }

  test("Suspicious path should be rejected") {
    withFiles { baseUrl =>
      val res = request.get(uri"$baseUrl/../text_file.txt").send()
      assertEquals(res.code, StatusCode.Forbidden)
    }
  }

}
