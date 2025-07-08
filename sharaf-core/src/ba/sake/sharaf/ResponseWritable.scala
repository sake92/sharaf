package ba.sake.sharaf

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.io.{FileInputStream, InputStream, OutputStream}
import scala.util.Using
import sttp.model.HeaderNames
import scalatags.Text.all.doctype
import scalatags.Text.Frag
import ba.sake.tupson.{JsonRW, toJson}

private val ContentTypeHttpString = HttpString(HeaderNames.ContentType)
private val ContentDispositionHttpString = HttpString(HeaderNames.ContentDisposition)

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

  // really handy when working with HTMX !
  given ResponseWritable[Frag] with {
    override def write(value: Frag, outputStream: OutputStream): Unit =
      ResponseWritable[String].write(value.render, outputStream)
    override def headers(value: Frag): Seq[(HttpString, Seq[String])] = Seq(
      ContentTypeHttpString -> Seq("text/html; charset=utf-8")
    )
  }

  given ResponseWritable[doctype] with {
    override def write(value: doctype, outputStream: OutputStream): Unit =
      ResponseWritable[String].write(value.render, outputStream)
    override def headers(value: doctype): Seq[(HttpString, Seq[String])] = Seq(
      ContentTypeHttpString -> Seq("text/html; charset=utf-8")
    )
  }

  given [T: JsonRW]: ResponseWritable[T] with {
    override def write(value: T, outputStream: OutputStream): Unit =
      ResponseWritable[String].write(value.toJson, outputStream)
    override def headers(value: T): Seq[(HttpString, Seq[String])] = Seq(
      ContentTypeHttpString -> Seq("application/json; charset=utf-8")
    )
  }

}

trait LowPriResponseWritableInstances {
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
