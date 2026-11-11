package ba.sake.sharaf.handlers

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import sttp.client4.quick.*
import sttp.model.{HeaderNames, StatusCode}
import ba.sake.sharaf.*

abstract class AbstractFileMimeTest extends munit.FunSuite with ServerFixture {
  test("static files cannot follow symlinks outside the root") {
    val directory = Files.createTempDirectory("sharaf-file-links")
    val root = Files.createDirectory(directory.resolve("root"))
    val inside = Files.write(root.resolve("inside.txt"), "inside".getBytes(UTF_8))
    val outside = Files.write(directory.resolve("outside.txt"), "outside".getBytes(UTF_8))
    val insideLink = Files.createSymbolicLink(root.resolve("inside-link.txt"), inside)
    val outsideLink = Files.createSymbolicLink(root.resolve("outside-link.txt"), outside)
    val outsideDirectory = Files.createSymbolicLink(root.resolve("outside-dir"), directory)
    val rootLink = Files.createSymbolicLink(directory.resolve("root-link"), root)
    val brokenLink = Files.createSymbolicLink(root.resolve("broken.txt"), directory.resolve("missing.txt"))
    try {
      withHandler(SharafHandler.exceptions(SharafHandler.files(rootLink))) { base =>
        val escaped = request.get(uri"$base/outside-link.txt").send()
        assertEquals(escaped.code, StatusCode.Forbidden)
        assertEquals(request.get(uri"$base/outside-dir/outside.txt").send().code, StatusCode.Forbidden)
        val allowed = request.get(uri"$base/inside-link.txt").send()
        assertEquals(allowed.code, StatusCode.Ok)
        assertEquals(allowed.body, "inside")
        assertEquals(request.get(uri"$base/broken.txt").send().code, StatusCode.NotFound)
        assertEquals(request.get(uri"$base/missing.txt").send().code, StatusCode.NotFound)
      }
    } finally {
      Seq(insideLink, outsideLink, outsideDirectory, rootLink, brokenLink, inside, outside, root, directory)
        .foreach(Files.deleteIfExists(_))
    }
  }

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
