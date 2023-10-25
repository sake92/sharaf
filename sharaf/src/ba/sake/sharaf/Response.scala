package ba.sake.sharaf

import scala.jdk.CollectionConverters.*

import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import io.undertow.util.HttpString

import ba.sake.hepek.html.HtmlPage
import ba.sake.tupson.*

case class Response[T] private (
    status: Int = 200,
    headers: Map[String, Seq[String]] = Map.empty,
    body: Option[T] = None
)(using val rw: ResponseWritable[T]) {

  def withStatus(status: Int) =
    copy(status = status)

  def withHeader(name: String, values: Seq[String]) =
    copy(headers = headers + (name -> values))
  def withHeader(name: String, value: String) =
    copy(headers = headers + (name -> Seq(value)))

  def withBody[T: ResponseWritable](body: T): Response[T] =
    copy(body = Some(body))
}

object Response {

  def withStatus(status: Int) =
    Response[String](status = status)

  def withHeader(name: String, values: Seq[String]) =
    Response[String](headers = Map(name -> values))
  def withHeader(name: String, value: String) =
    Response[String](headers = Map(name -> Seq(value)))

  def withBody[T: ResponseWritable](body: T): Response[T] =
    Response(body = Some(body))
  def withBodyOpt[T: ResponseWritable](body: Option[T], name: String): Response[T] = body match
    case Some(value) => withBody(value)
    case None        => throw NotFoundException(name)

  def redirect(location: String): Response[String] =
    withStatus(301).withHeader("Location", location)

}

trait ResponseWritable[T] {
  def write(value: T, exchange: HttpServerExchange): Unit
  def headers(value: T): Seq[(String, Seq[String])]
}

object ResponseWritable {

  private[sharaf] def writeResponse(response: Response[?], exchange: HttpServerExchange): Unit = {
    // headers
    val allHeaders = response.body.flatMap(response.rw.headers) ++ response.headers
    allHeaders.foreach { case (name, values) =>
      exchange.getResponseHeaders.putAll(new HttpString(name), values.asJava)
    }
    // status code
    exchange.setStatusCode(response.status)
    // body
    response.body.foreach(b => response.rw.write(b, exchange))
  }

  /* instances */
  given ResponseWritable[String] = new {
    override def write(value: String, exchange: HttpServerExchange): Unit =
      exchange.getResponseSender.send(value)
    override def headers(value: String): Seq[(String, Seq[String])] = Seq(
      Headers.CONTENT_TYPE_STRING -> Seq("text/plain")
    )
  }

  given ResponseWritable[HtmlPage] = new {
    override def write(value: HtmlPage, exchange: HttpServerExchange): Unit =
      val htmlText = "<!DOCTYPE html>" + value.contents
      exchange.getResponseSender.send(htmlText)
    override def headers(value: HtmlPage): Seq[(String, Seq[String])] = Seq(
      Headers.CONTENT_TYPE_STRING -> Seq("text/html; charset=utf-8")
    )
  }

  given [T: JsonRW]: ResponseWritable[T] = new {
    override def write(value: T, exchange: HttpServerExchange): Unit =
      exchange.getResponseSender.send(value.toJson)
    override def headers(value: T): Seq[(String, Seq[String])] = Seq(
      Headers.CONTENT_TYPE_STRING -> Seq("application/json")
    )
  }

}
