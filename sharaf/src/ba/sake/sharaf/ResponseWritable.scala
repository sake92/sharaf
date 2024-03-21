package ba.sake.sharaf

import scala.jdk.CollectionConverters.*
import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString
import io.undertow.util.Headers
import scalatags.Text.Frag
import ba.sake.hepek.html.HtmlPage
import ba.sake.tupson.*

trait ResponseWritable[-T]:
  def write(value: T, exchange: HttpServerExchange): Unit
  def headers(value: T): Seq[(HttpString, Seq[String])]

object ResponseWritable {

  private[sharaf] def writeResponse(response: Response[?], exchange: HttpServerExchange): Unit = {
    // headers
    val bodyContentHeaders = response.body.flatMap(response.rw.headers)
    bodyContentHeaders.foreach { case (name, values) =>
      exchange.getResponseHeaders.putAll(name, values.asJava)
    }

    response.headerUpdates.updates.foreach {
      case HeaderUpdate.Set(name, values) =>
        exchange.getResponseHeaders.remove(name)
        exchange.getResponseHeaders.addAll(name, values.asJava)
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

  // really handy when working with HTMX !
  given ResponseWritable[Frag] with {
    override def write(value: Frag, exchange: HttpServerExchange): Unit =
      val htmlText = value.render
      exchange.getResponseSender.send(htmlText)
    override def headers(value: Frag): Seq[(HttpString, Seq[String])] = Seq(
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
