package ba.sake.sharaf

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.io.{FileInputStream, InputStream, OutputStream}
import scala.util.Using
import sttp.model.HeaderNames
import ba.sake.tupson.{JsonRW, toJson}

private val ContentTypeHttpString = HttpString(HeaderNames.ContentType)
private val ContentDispositionHttpString = HttpString(HeaderNames.ContentDisposition)
private val CacheControlHttpString = HttpString(HeaderNames.CacheControl)
private val ConnectionHttpString = HttpString(HeaderNames.Connection)

trait ResponseWritable[-T]:
  def write(value: T, outputStream: OutputStream): Unit
  def headers(value: T): Seq[(HttpString, Seq[String])]

object ResponseWritable extends LowPriResponseWritableInstances {

  def apply[T](using rw: ResponseWritable[T]): ResponseWritable[T] = rw

  /* instances */
  given ResponseWritable[String] with {
    override def write(value: String, outputStream: OutputStream): Unit =
      outputStream.write(value.getBytes(StandardCharsets.UTF_8))
    override def headers(value: String): Seq[(HttpString, Seq[String])] = Seq(
      ContentTypeHttpString -> Seq("text/plain; charset=utf-8")
    )
  }

  given ResponseWritable[InputStream] with {
    override def write(value: InputStream, outputStream: OutputStream): Unit =
      Using.resource(value) { is =>
        is.transferTo(outputStream)
      }

    // application/octet-stream says "it can be anything"
    override def headers(value: InputStream): Seq[(HttpString, Seq[String])] = Seq(
      ContentTypeHttpString -> Seq("application/octet-stream")
    )
  }

  given ResponseWritable[Path] with {
    override def write(value: Path, outputStream: OutputStream): Unit =
      ResponseWritable[InputStream].write(
        new FileInputStream(value.toFile),
        outputStream
      )

    // https://stackoverflow.com/questions/20508788/do-i-need-content-type-application-octet-stream-for-file-download
    override def headers(value: Path): Seq[(HttpString, Seq[String])] = Seq(
      ContentTypeHttpString -> Seq("application/octet-stream"),
      ContentDispositionHttpString -> Seq(s""" attachment; filename="${value.getFileName}" """.trim)
    )
  }

  given [T: JsonRW]: ResponseWritable[T] with {
    override def write(value: T, outputStream: OutputStream): Unit =
      ResponseWritable[String].write(value.toJson, outputStream)
    override def headers(value: T): Seq[(HttpString, Seq[String])] = Seq(
      ContentTypeHttpString -> Seq("application/json; charset=utf-8")
    )
  }

  given ResponseWritable[SseSender] with {
    override def write(value: SseSender, outputStream: OutputStream): Unit = {
      var done = false
      while !done do {
        val event = value.queue.take()
        done = event.isInstanceOf[ServerSentEvent.Done]
        outputStream.write(event.sseBytes)
        outputStream.flush()
      }
    }

    override def headers(value: SseSender): Seq[(HttpString, Seq[String])] = Seq(
      ContentTypeHttpString -> Seq("text/event-stream"),
      CacheControlHttpString -> Seq("no-cache"),
      ConnectionHttpString -> Seq("keep-alive")
    )
  }
  
  given ResponseWritable[geny.Writable] with {
    override def write(value: geny.Writable, outputStream: OutputStream): Unit =
      value.writeBytesTo(outputStream)

    // application/octet-stream says "it can be anything"
    override def headers(value: geny.Writable): Seq[(HttpString, Seq[String])] =
      Seq(
        ContentTypeHttpString -> Seq(value.httpContentType.getOrElse("application/octet-stream"))
      )
  }
}

trait LowPriResponseWritableInstances {}
