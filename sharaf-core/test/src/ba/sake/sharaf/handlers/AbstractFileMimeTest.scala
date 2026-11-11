package ba.sake.sharaf.handlers

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import sttp.client4.quick.*
import sttp.model.{HeaderNames, StatusCode}
import ba.sake.sharaf.*

abstract class AbstractFileMimeTest extends munit.FunSuite with ServerFixture {
  test("static files use extension-based content types") {
    val examples = Seq(
      (name = "page.html", mime = "text/html"),
      (name = "style.CSS", mime = "text/css"),
      (name = "app.js", mime = "text/javascript"),
      (name = "data.json", mime = "application/json"),
      (name = "picture.svg", mime = "image/svg+xml"),
      (name = "picture.png", mime = "image/png"),
      (name = "font.woff2", mime = "font/woff2"),
      (name = "unknown.extension", mime = "application/octet-stream"),
      (name = "noextension", mime = "application/octet-stream")
    )
    val root = Files.createTempDirectory("sharaf-mime-test")
    try {
      examples.foreach(example => Files.write(root.resolve(example.name), "file contents".getBytes(UTF_8)))
      withHandler(SharafHandler.exceptions(SharafHandler.files(root))) { base =>
        examples.foreach { example =>
          val response = request.get(uri"$base/${example.name}").send()
          assertEquals(response.code, StatusCode.Ok, example.name)
          assertEquals(response.body, "file contents", example.name)
          assertEquals(response.header(HeaderNames.ContentType), Some(example.mime), example.name)
        }
        val initial = request.get(uri"$base/page.html").send()
        val etag = initial.header(HeaderNames.Etag).getOrElse(fail("Expected ETag"))
        assert(etag.startsWith("W/\""))
        val cached = request.get(uri"$base/page.html").header(HeaderNames.IfNoneMatch, etag).send()
        assertEquals(cached.code, StatusCode.NotModified)
      }
    } finally {
      examples.foreach(example => Files.deleteIfExists(root.resolve(example.name)))
      Files.deleteIfExists(root)
    }
  }
}
