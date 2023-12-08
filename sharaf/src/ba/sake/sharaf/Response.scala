package ba.sake.sharaf

import scala.jdk.CollectionConverters.*

import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import io.undertow.util.HttpString
import io.undertow.util.StatusCodes
import ba.sake.hepek.html.HtmlPage
import ba.sake.tupson.*

final class Response[T] private (
    val status: Int,
    val headers: Map[String, Seq[String]],
    val body: Option[T]
)(using val rw: ResponseWritable[T]) {

  def withStatus(status: Int) =
    copy(status = status)

  def withHeader(name: String, values: Seq[String]) =
    copy(headers = headers + (name -> values))
  def withHeader(name: String, value: String) =
    copy(headers = headers + (name -> Seq(value)))

  def withBody[T2: ResponseWritable](body: T2): Response[T2] =
    copy(body = Some(body))

  private def copy[T2](
      status: Int = status,
      headers: Map[String, Seq[String]] = headers,
      body: Option[T2] = body
  )(using ResponseWritable[T2]) = new Response(status, headers, body)
}

object Response {

  def apply[T: ResponseWritable] = new Response(StatusCodes.OK, Map.empty, None)

  def withStatus(status: Int) =
    Response[String].withStatus(status)

  def withHeader(name: String, values: Seq[String]) =
    Response[String].withHeader(name, values)

  def withHeader(name: String, value: String) =
    Response[String].withHeader(name, Seq(value))

  def withBody[T: ResponseWritable](body: T): Response[T] =
    Response[String].withBody(body)

  def withBodyOpt[T: ResponseWritable](body: Option[T], name: String): Response[T] = body match
    case Some(value) => withBody(value)
    case None        => throw NotFoundException(name)

  def redirect(location: String): Response[String] =
    withStatus(StatusCodes.MOVED_PERMANENTLY).withHeader("Location", location)

}

trait ResponseWritable[-T]:
  def write(value: T, exchange: HttpServerExchange): Unit
  def headers(value: T): Seq[(String, Seq[String])]

object ResponseWritable {

  private[sharaf] def writeResponse(response: Response[?], exchange: HttpServerExchange): Unit = {
    // headers
    val allHeaders = response.body.flatMap(response.rw.headers) ++ response.headers
    allHeaders.foreach { case (name, values) =>
      exchange.getResponseHeaders.putAll(HttpString(name), values.asJava)
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
