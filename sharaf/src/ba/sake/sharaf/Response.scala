package ba.sake.sharaf

import scala.jdk.CollectionConverters.*

import io.undertow.io.IoCallback
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import io.undertow.util.HttpString
import io.undertow.util.MimeMappings

import ba.sake.hepek.html.HtmlPage
import ba.sake.tupson.*

case class Response[T] private (
    body: T,
    status: Int = 200,
    headers: Map[String, Seq[String]] = Map.empty
)(using val rw: ResponseWritable[T]) {

  def withStatus(status: Int) = copy(status = status)

  def withHeader(name: String, values: Seq[String]) =
    copy(headers = headers + (name -> values))
  def withHeader(name: String, value: String) =
    copy(headers = headers + (name -> Seq(value)))
}

object Response {

  def withBody[T: ResponseWritable](body: T): Response[T] =
    Response(body)
  def withBodyOpt[T: ResponseWritable](body: Option[T], name: String): Response[T] = body match
    case Some(value) => withBody(value)
    case None        => throw NotFoundException(name)

  def redirect(location: String): Response[String] =
    withBody("").withStatus(301).withHeader("Location", location)

}

trait ResponseWritable[T] {
  def write(value: T, exchange: HttpServerExchange): Unit
  def headers(value: T): Seq[(String, Seq[String])]
}

object ResponseWritable {

  private[sharaf] def writeResponse(response: Response[?], exchange: HttpServerExchange): Unit = {
    // headers
    val allHeaders = response.rw.headers(response.body) ++ response.headers
    allHeaders.foreach { case (name, values) =>
      exchange.getResponseHeaders.putAll(new HttpString(name), values.asJava)
    }
    // status code
    exchange.setStatusCode(response.status)
    // body
    response.rw.write(response.body, exchange)
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

  given ResponseWritable[Resource] = new {
    override def write(value: Resource, exchange: HttpServerExchange): Unit = value match
      case res: Resource.ClasspathResource =>
        res.underlying.serve(exchange.getResponseSender(), exchange, IoCallback.END_EXCHANGE)

    override def headers(value: Resource): Seq[(String, Seq[String])] = value match
      case res: Resource.ClasspathResource => {
        val contentType = res.underlying.getContentType(MimeMappings.DEFAULT)
        Seq(
          Headers.CONTENT_TYPE_STRING -> Seq(contentType)
        )
      }
  }

  given [T: JsonRW]: ResponseWritable[T] = new {
    override def write(value: T, exchange: HttpServerExchange): Unit =
      exchange.getResponseSender.send(value.toJson)
    override def headers(value: T): Seq[(String, Seq[String])] = Seq(
      Headers.CONTENT_TYPE_STRING -> Seq("application/json")
    )
  }

}
