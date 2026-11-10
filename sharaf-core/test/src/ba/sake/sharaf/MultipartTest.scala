package ba.sake.sharaf

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import ba.sake.formson.*
import ba.sake.sharaf.exceptions.*

class MultipartTest extends munit.FunSuite {
  private val contentType = "Multipart/Form-Data; boundary=\"test-boundary\""
  private def bytes(value: String): Array[Byte] = value.getBytes(UTF_8)
  private def field(name: String, value: String): String =
    s"--test-boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n"
  private def fileHeader: String =
    "--test-boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a;\\\"b.bin\"\r\nContent-Type: application/octet-stream\r\nContent-Transfer-Encoding: binary\r\n\r\n"
  private val end = "--test-boundary--\r\n"

  test("quoted boundaries preserve key order, repeated and empty fields, and Unicode") {
    val uploads = new MultipartUploads
    try {
      val raw = field("first", "čćž") + field("", "") + field("first", "two=+&") + end
      val form = Multipart.parse(new ByteArrayInputStream(bytes(raw)), contentType, uploads)
      assertEquals(form.keys.toSeq, Seq("first", ""))
      assertEquals(form("first"), Seq(FormValue.Str("čćž"), FormValue.Str("two=+&")))
      assertEquals(form(""), Seq(FormValue.Str("")))
    } finally uploads.close()
  }

  test("preamble, epilogue and transport padding on boundary lines are accepted") {
    val raw = "preamble\r\n" + field("name", "hello").replace("--test-boundary\r\n", "--test-boundary \t\r\n") +
      "--test-boundary-- \t\r\nepilogue"
    val uploads = new MultipartUploads
    try {
      val form = Multipart.parse(new ByteArrayInputStream(bytes(raw)), contentType, uploads)
      assertEquals(form("name"), Seq(FormValue.Str("hello")))
    } finally uploads.close()
  }

  test("binary uploads and boundary-like payload bytes are preserved even with one-byte reads") {
    val payload = (0 until 20000).map(i => (i * 37).toByte).toArray ++
      bytes("\r\n--test-boundaryX\r\n--test-boundary--junk\r\n--test-boundar\r\n--test-boundary-\r\n")
    val raw = bytes(fileHeader) ++ payload ++ bytes("\r\n" + end)
    val source = new ByteArrayInputStream(raw) {
      override def read(b: Array[Byte], off: Int, len: Int): Int = super.read(b, off, math.min(len, 1))
    }
    val uploads = new MultipartUploads
    val path =
      try {
        val form = Multipart.parse(source, contentType, uploads)
        val path = form("file").head.asInstanceOf[FormValue.File].value
        assertEquals(Files.readAllBytes(path).toSeq, payload.toSeq)
        path
      } finally uploads.close()
    assert(!Files.exists(path))
    uploads.close()
  }

  test("empty and repeated uploads are always distinct temporary Path files") {
    val uploads = new MultipartUploads
    val paths =
      try {
        val raw = fileHeader + "\r\n" + fileHeader + "x\r\n" + end
        val form = Multipart.parse(new ByteArrayInputStream(bytes(raw)), contentType, uploads)
        val paths = form("file").map(_.asInstanceOf[FormValue.File].value)
        assertEquals(paths.distinct.size, 2)
        assertEquals(paths.map(Files.size), Seq(0L, 1L))
        paths
      } finally uploads.close()
    assert(paths.forall(path => !Files.exists(path)))
  }

  test("malformed input and truncation clean up all already-created uploads") {
    val valid = bytes(fileHeader + "payload\r\n" + field("next", "text") + end)
    for raw <- Seq(valid.dropRight(8), bytes(fileHeader + "payload\r\n--test-boundary\r\nbroken\r\n\r\nx\r\n" + end))
    do {
      val directory = Files.createTempDirectory("sharaf-multipart-test-")
      val uploads = new MultipartUploads(Some(directory))
      intercept[BadRequestException] {
        Multipart.parse(new ByteArrayInputStream(raw), contentType, uploads)
      }
      val remaining = Files.list(directory)
      try assertEquals(remaining.count(), 0L)
      finally remaining.close()
      Files.delete(directory)
      uploads.close()
    }
  }

  test("all parser limits reject with 413 and release owned files") {
    val raw = bytes(fileHeader + "abcdef\r\n" + field("name", "hello") + end)
    val limits = Seq(
      Multipart.Limits(bodyBytes = raw.length - 1),
      Multipart.Limits(fileBytes = 5),
      Multipart.Limits(fieldBytes = 4),
      Multipart.Limits(parts = 1),
      Multipart.Limits(headerBytes = 100),
      Multipart.Limits(headers = 1)
    )
    for limit <- limits do {
      val directory = Files.createTempDirectory("sharaf-multipart-limit-")
      val uploads = new MultipartUploads(Some(directory))
      intercept[PayloadTooLargeException] {
        Multipart.parse(new ByteArrayInputStream(raw), contentType, uploads, limit)
      }
      val remaining = Files.list(directory)
      try assertEquals(remaining.count(), 0L)
      finally remaining.close()
      Files.delete(directory)
    }
  }

  test("exact body and part limits succeed, while epilogue bytes count towards the total") {
    val raw = bytes(fileHeader + "abcdef\r\n" + field("name", "hello") + end)
    val uploads = new MultipartUploads
    try {
      val form = Multipart.parse(
        new ByteArrayInputStream(raw),
        contentType,
        uploads,
        Multipart.Limits(bodyBytes = raw.length, fileBytes = 6, fieldBytes = 5, parts = 2)
      )
      assertEquals(form("name"), Seq(FormValue.Str("hello")))
      assertEquals(Files.size(form("file").head.asInstanceOf[FormValue.File].value), 6L)
    } finally uploads.close()
    intercept[PayloadTooLargeException] {
      Multipart.parse(
        new ByteArrayInputStream(raw ++ bytes("epilogue")),
        contentType,
        new MultipartUploads,
        Multipart.Limits(bodyBytes = raw.length)
      )
    }
  }

  test("malformed boundaries and headers are rejected") {
    for ct <- Seq(
        "multipart/form-data",
        "multipart/form-data; boundary=\"\"",
        "multipart/form-data; boundary=\"bad@boundary\"",
        "multipart/form-data; boundary=x; boundary=y",
        "multipart/form-data; boundary=\"unclosed"
      )
    do {
      intercept[BadRequestException] {
        Multipart.parse(new ByteArrayInputStream(Array.emptyByteArray), ct, new MultipartUploads)
      }
    }
    for header <- Seq(
        "Content-Disposition: form-data",
        "Content-Disposition: attachment; name=x",
        "Content-Disposition: form-data; name=x\r\nContent-Disposition: form-data; name=y",
        "Content-Disposition: form-data; name=x\r\nContent-Transfer-Encoding: base64",
        " invalid: value"
      )
    do {
      val raw = s"--test-boundary\r\n$header\r\n\r\nx\r\n$end"
      intercept[BadRequestException] {
        Multipart.parse(new ByteArrayInputStream(bytes(raw)), contentType, new MultipartUploads)
      }
    }
  }

  test("I/O failures during an upload remove partial files and propagate the original error") {
    val directory = Files.createTempDirectory("sharaf-multipart-io-")
    val uploads = new MultipartUploads(Some(directory))
    val failure = new java.io.IOException("broken input")
    val prefix = bytes(fileHeader + "partial")
    val input = new InputStream {
      private var index = 0
      override def read(): Int = {
        if index == prefix.length then throw failure
        val value = prefix(index) & 255
        index += 1
        value
      }
      override def read(b: Array[Byte], off: Int, len: Int): Int = {
        b(off) = read().toByte
        1
      }
    }
    try {
      assert(intercept[java.io.IOException] { Multipart.parse(input, contentType, uploads) } eq failure)
      val remaining = Files.list(directory)
      try assertEquals(remaining.count(), 0L)
      finally remaining.close()
    } finally {
      uploads.close()
      Files.delete(directory)
    }
  }

  test("typed multipart conversion preserves repeated fields as parts on JVM and Native") {
    case class Form(name: String, tag: Seq[String]) derives FormDataRW
    val parts = Form("hello", Seq("first", "second")).toSttpMultipart(DefaultFormsonConfig.withSeqNoBrackets)
    assertEquals(parts.map(_.name), Seq("name", "tag", "tag"))
  }

  test("default and JSON mappers return 413 for parser limits") {
    val error = PayloadTooLargeException("Multipart form limit exceeded")
    assertEquals(ExceptionMapper.default(error).status.code, 413)
    assertEquals(ExceptionMapper.json(error).status.code, 413)
  }
}
