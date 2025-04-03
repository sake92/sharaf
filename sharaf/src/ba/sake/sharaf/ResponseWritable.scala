package ba.sake.sharaf

import java.nio.file.Path
import java.io.{FileInputStream, InputStream}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*
import scala.util.Using
import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString
import io.undertow.util.Headers
import scalatags.Text.all.doctype
import scalatags.Text.Frag
import ba.sake.hepek.html.HtmlPage
import ba.sake.tupson.{JsonRW, toJson}

trait ResponseWritable[-T]:
  def write(value: T, exchange: HttpServerExchange): Unit
  def headers(value: T): Seq[(HttpString, Seq[String])]

object ResponseWritable extends LowPriResponseWritableInstances {
  
  def apply[T](using rw: ResponseWritable[T]): ResponseWritable[T] = rw

  private[sharaf] def writeResponse(response: Response[?], exchange: HttpServerExchange): Unit = {
    // headers
    val bodyContentHeaders = response.body.flatMap(response.rw.headers)
    bodyContentHeaders.foreach { case (name, values) =>
      exchange.getResponseHeaders.putAll(name, values.asJava)
    }

    response.headerUpdates.updates.foreach {
      case HeaderUpdate.Set(name, values) =>
        exchange.getResponseHeaders.putAll(name, values.asJava)
      case HeaderUpdate.Remove(name) =>
        exchange.getResponseHeaders.remove(name)
    }

    // status code
    exchange.setStatusCode(response.status)
    // body
    response.body.foreach(b => response.rw.write(b, exchange))
  }

  /* instances */
  given ResponseWritable[String] with {
    override def write(value: String, exchange: HttpServerExchange): Unit =
      exchange.getResponseSender.send(value)
    override def headers(value: String): Seq[(HttpString, Seq[String])] = Seq(
      Headers.CONTENT_TYPE -> Seq("text/plain")
    )
  }

  given ResponseWritable[InputStream] with {
    override def write(value: InputStream, exchange: HttpServerExchange): Unit =
      Using.resources(value, exchange.getOutputStream) { (is, os) =>
        is.transferTo(os)
      }

    // application/octet-stream says "it can be anything"
    override def headers(value: InputStream): Seq[(HttpString, Seq[String])] = Seq(
      Headers.CONTENT_TYPE -> Seq("application/octet-stream")
    )
  }

  given ResponseWritable[Path] with {
    override def write(value: Path, exchange: HttpServerExchange): Unit =
      ResponseWritable[InputStream].write(
        new FileInputStream(value.toFile),
        exchange
      )

    // https://stackoverflow.com/questions/20508788/do-i-need-content-type-application-octet-stream-for-file-download
    override def headers(value: Path): Seq[(HttpString, Seq[String])] = Seq(
      Headers.CONTENT_TYPE -> Seq("application/octet-stream"),
      Headers.CONTENT_DISPOSITION -> Seq(s""" attachment; filename="${value.getFileName}" """.trim)
    )
  }

  // really handy when working with HTMX !
  given ResponseWritable[Frag] with {
    override def write(value: Frag, exchange: HttpServerExchange): Unit =
      val htmlText = value.render
      exchange.getResponseSender.send(htmlText)
    override def headers(value: Frag): Seq[(HttpString, Seq[String])] = Seq(
      Headers.CONTENT_TYPE -> Seq("text/html; charset=utf-8")
    )
  }

  given ResponseWritable[doctype] with {
    override def write(value: doctype, exchange: HttpServerExchange): Unit =
      exchange.getResponseSender.send(value.render)
    override def headers(value: doctype): Seq[(HttpString, Seq[String])] = Seq(
      Headers.CONTENT_TYPE -> Seq("text/html; charset=utf-8")
    )
  }

  given ResponseWritable[HtmlPage] with {
    override def write(value: HtmlPage, exchange: HttpServerExchange): Unit =
      val htmlText = "<!DOCTYPE html>" + value.contents
      exchange.getResponseSender.send(htmlText)
    override def headers(value: HtmlPage): Seq[(HttpString, Seq[String])] = Seq(
      Headers.CONTENT_TYPE -> Seq("text/html; charset=utf-8")
    )
  }

  given [T: JsonRW]: ResponseWritable[T] with {
    override def write(value: T, exchange: HttpServerExchange): Unit =
      exchange.getResponseSender.send(value.toJson)
    override def headers(value: T): Seq[(HttpString, Seq[String])] = Seq(
      Headers.CONTENT_TYPE -> Seq("application/json")
    )
  }

}

trait LowPriResponseWritableInstances {
  given ResponseWritable[geny.Writable] with {
    override def write(value: geny.Writable, exchange: HttpServerExchange): Unit =
      value.writeBytesTo(exchange.getOutputStream)

    // application/octet-stream says "it can be anything"
    override def headers(value: geny.Writable): Seq[(HttpString, Seq[String])] = Seq(
      Headers.CONTENT_TYPE -> Seq("application/octet-stream")
    )
  }
}