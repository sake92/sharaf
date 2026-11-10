package ba.sake.sharaf

import java.io.{
  BufferedInputStream,
  BufferedOutputStream,
  ByteArrayOutputStream,
  InputStream,
  OutputStream,
  PushbackInputStream
}
import java.nio.charset.StandardCharsets.{ISO_8859_1, UTF_8}
import java.nio.file.{Files, Path}
import scala.collection.immutable.SeqMap
import scala.collection.mutable
import scala.util.control.NonFatal
import ba.sake.formson.*
import ba.sake.sharaf.exceptions.*

/** Request-owned uploads. Adapters must close this after response serialization, including failures. */
private[sharaf] final class MultipartUploads(directory: Option[Path] = None) extends AutoCloseable {
  private val paths = mutable.ArrayBuffer.empty[Path]

  def create(): Path = {
    val path = directory match {
      case Some(dir) => Files.createTempFile(dir, "sharaf-upload-", ".tmp")
      case None      => Files.createTempFile("sharaf-upload-", ".tmp")
    }
    paths += path
    path
  }

  override def close(): Unit = {
    var failure: Throwable = null
    paths.foreach { path =>
      try Files.deleteIfExists(path)
      catch
        case NonFatal(error) =>
          if failure == null then failure = error else failure.addSuppressed(error)
    }
    paths.clear()
    if failure != null then throw failure
  }
}

/** Byte-oriented, bounded multipart/form-data parser, shared by JVM and Native adapters. */
private[sharaf] object Multipart {
  case class Limits(
      bodyBytes: Long = 32L * 1024 * 1024,
      fileBytes: Long = 16L * 1024 * 1024,
      fieldBytes: Long = 64L * 1024,
      parts: Int = 128,
      headerBytes: Int = 16 * 1024,
      headers: Int = 32
  )

  def isMultipart(contentType: String): Boolean =
    contentType.takeWhile(_ != ';').trim.equalsIgnoreCase("multipart/form-data")

  def parse(
      input: InputStream,
      contentType: String,
      uploads: MultipartUploads,
      limits: Limits = Limits()
  ): FormDataMap = {
    try new Parser(input, boundary(contentType), uploads, limits).parse()
    catch
      case NonFatal(error) =>
        try uploads.close()
        catch case NonFatal(cleanup) => error.addSuppressed(cleanup)
        throw error
  }

  private def invalid(): Nothing = throw BadRequestException("Malformed multipart/form-data")
  private def tooLarge(): Nothing = throw PayloadTooLargeException("Multipart form limit exceeded")

  private def token(char: Char): Boolean =
    char.isLetterOrDigit && char <= 127 || "!#$%&'*+-.^_`|~".contains(char)

  /** Parse parameters without splitting quoted values containing semicolons or escaped quotes. */
  private def parameters(value: String): (kind: String, values: Map[String, String]) = {
    val end = value.indexOf(';') match {
      case -1 => value.length
      case n  => n
    }
    val kind = value.take(end).trim.toLowerCase(java.util.Locale.ROOT)
    var index = end
    val params = mutable.Map.empty[String, String]
    def whitespace(): Unit = while index < value.length && (value(index) == ' ' || value(index) == '\t') do index += 1
    while index < value.length do {
      if value(index) != ';' then invalid()
      index += 1
      whitespace()
      val start = index
      while index < value.length && token(value(index)) do index += 1
      if index == start then invalid()
      val name = value.substring(start, index).toLowerCase(java.util.Locale.ROOT)
      whitespace()
      if index == value.length || value(index) != '=' then invalid()
      index += 1
      whitespace()
      val result = new StringBuilder
      if index < value.length && value(index) == '"' then {
        index += 1
        while index < value.length && value(index) != '"' do {
          if value(index) == '\\' then index += 1
          if index == value.length || value(index) < ' ' || value(index) == 127 then invalid()
          result.append(value(index))
          index += 1
        }
        if index == value.length then invalid()
        index += 1
      } else {
        while index < value.length && token(value(index)) do {
          result.append(value(index))
          index += 1
        }
        if result.isEmpty then invalid()
      }
      whitespace()
      if params.contains(name) then invalid()
      params(name) = result.toString
    }
    (kind = kind, values = params.toMap)
  }

  private def boundary(contentType: String): String = {
    val parsed = parameters(contentType)
    if parsed.kind != "multipart/form-data" then invalid()
    val value = parsed.values.getOrElse("boundary", invalid())
    if value.isEmpty || value.length > 70 || value.last == ' ' ||
      !value.forall(c => c >= ' ' && c <= '~' && (c.isLetterOrDigit || "'()+_,-./:=? ".contains(c)))
    then invalid()
    value
  }

  private final class Parser(source: InputStream, boundary: String, uploads: MultipartUploads, limits: Limits) {
    private var total = 0L
    private val bounded = new InputStream {
      override def read(): Int = {
        val value = source.read()
        if value >= 0 then {
          total += 1
          if total > limits.bodyBytes then tooLarge()
        }
        value
      }
      override def read(bytes: Array[Byte], offset: Int, length: Int): Int = {
        val allowed = math.min(length.toLong, math.max(1L, limits.bodyBytes - total + 1)).toInt
        val count = source.read(bytes, offset, allowed)
        if count > 0 then {
          total += count
          if total > limits.bodyBytes then tooLarge()
        }
        count
      }
    }
    private val delimiter = ("\r\n--" + boundary).getBytes(ISO_8859_1)
    private val lookaheadBytes = delimiter.length + limits.headerBytes + 4
    private val input = new PushbackInputStream(new BufferedInputStream(bounded), lookaheadBytes)

    def parse(): FormDataMap = {
      def delimiterLine(): String = line(limits.headerBytes).reverse.dropWhile(c => c == ' ' || c == '\t').reverse
      var first = delimiterLine()
      while first != "--" + boundary && first != "--" + boundary + "--" do first = delimiterLine()
      var finished = first == "--" + boundary + "--"
      var count = 0
      val result = mutable.LinkedHashMap.empty[String, Seq[FormValue]]
      while !finished do {
        count += 1
        if count > limits.parts then tooLarge()
        val headers = readHeaders()
        val disposition = parameters(headers.getOrElse("content-disposition", invalid()))
        if disposition.kind != "form-data" then invalid()
        val name = disposition.values.getOrElse("name", invalid())
        headers.get("content-transfer-encoding").foreach { encoding =>
          if !Seq("binary", "8bit", "7bit").exists(_.equalsIgnoreCase(encoding)) then invalid()
        }
        val value = if disposition.values.contains("filename") then {
          val path = uploads.create()
          val output = new BufferedOutputStream(Files.newOutputStream(path))
          try finished = part(output, limits.fileBytes)
          finally output.close()
          FormValue.File(path)
        } else {
          val output = new ByteArrayOutputStream()
          finished = part(output, limits.fieldBytes)
          FormValue.Str(new String(output.toByteArray, UTF_8))
        }
        result(name) = result.getOrElse(name, Seq.empty) :+ value
      }
      // Consume the epilogue too, so the total-body limit applies to the entire entity.
      while input.read() != -1 do ()
      SeqMap.from(result)
    }

    private def line(maxBytes: Int): String = {
      val bytes = new ByteArrayOutputStream()
      var done = false
      while !done do {
        val value = input.read()
        if value == -1 then invalid()
        if value == 13 then {
          if input.read() != 10 then invalid()
          done = true
        } else {
          if value == 10 || value == 0 then invalid()
          bytes.write(value)
        }
        if bytes.size() + 2 > maxBytes then tooLarge()
      }
      new String(bytes.toByteArray, UTF_8)
    }

    private def readHeaders(): Map[String, String] = {
      val headers = mutable.Map.empty[String, String]
      var remaining = limits.headerBytes
      var count = 0
      var done = false
      while !done do {
        val text = line(remaining)
        remaining -= text.getBytes(UTF_8).length + 2
        if text.isEmpty then done = true
        else {
          count += 1
          if count > limits.headers then tooLarge()
          val colon = text.indexOf(':')
          if colon <= 0 || !text.take(colon).forall(token) then invalid()
          val name = text.take(colon).toLowerCase(java.util.Locale.ROOT)
          val value = text.drop(colon + 1).trim
          if headers.contains(name) || value.exists(c => c < ' ' && c != '\t' || c == 127) then invalid()
          headers(name) = value
        }
      }
      headers.toMap
    }

    /** A candidate delimiter only ends a part when followed by CRLF or a closing '--'. */
    private def part(output: OutputStream, maxBytes: Long): Boolean = {
      var size = 0L
      var done = false
      var last = false
      val candidate = new Array[Byte](lookaheadBytes)
      while !done do {
        val value = input.read()
        if value == -1 then invalid()
        var matched = false
        if value == (delimiter(0) & 255) then {
          candidate(0) = value.toByte
          var length = 1
          while length < delimiter.length && candidate(length - 1) == delimiter(length - 1) do {
            val next = input.read()
            if next == -1 then invalid()
            candidate(length) = next.toByte
            length += 1
          }
          if length == delimiter.length && candidate(length - 1) == delimiter.last then {
            def readCandidate(): Int = {
              val next = input.read()
              if next != -1 then {
                if length == candidate.length then tooLarge()
                candidate(length) = next.toByte
                length += 1
              }
              next
            }
            var next = readCandidate()
            var closing = false
            var validSuffix = true
            if next == 45 then {
              validSuffix = readCandidate() == 45
              if validSuffix then {
                closing = true
                next = readCandidate()
              }
            }
            if validSuffix then {
              // RFC 2046 transport padding is allowed, with bounded lookahead.
              var padding = 0
              while next == 32 || next == 9 do {
                padding += 1
                if padding > limits.headerBytes then tooLarge()
                next = readCandidate()
              }
              matched = if next == 13 then readCandidate() == 10 else next == -1 && closing
              if matched then last = closing
            }
          }
          if !matched then input.unread(candidate, 1, length - 1)
        }
        if matched then done = true
        else {
          size += 1
          if size > maxBytes then tooLarge()
          output.write(value)
        }
      }
      last
    }
  }
}
