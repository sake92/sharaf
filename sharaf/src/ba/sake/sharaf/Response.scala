package ba.sake.sharaf

import io.undertow.util.Headers
import ba.sake.tupson.*
import io.undertow.server.handlers.resource.URLResource
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Files
import io.undertow.util.HttpString
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.resource.ResourceHandler
import ba.sake.hepek.html.HtmlPage


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
}


trait ResponseWritable[T] {
  def write(value: T, exchange: HttpServerExchange): Unit
  def headers: Seq[(String, Seq[String])]
}


object ResponseWritable {
  given ResponseWritable[String] = new {
    override def write(value: String, exchange: HttpServerExchange): Unit =
      exchange.getResponseSender.send(value)
    override def headers: Seq[(String, Seq[String])] = Seq(
      Headers.CONTENT_TYPE_STRING -> Seq("text/plain")
    )
  }
  given ResponseWritable[HtmlPage] = new {
    override def write(value: HtmlPage, exchange: HttpServerExchange): Unit =
      val htmlText= "<!DOCTYPE html>" + value.contents
      exchange.getResponseSender.send(htmlText)
    override def headers: Seq[(String, Seq[String])] = Seq(
      Headers.CONTENT_TYPE_STRING -> Seq("text/html; charset=utf-8")
    )
  }
  given [T: JsonRW]: ResponseWritable[T] = new {
    override def write(value: T, exchange: HttpServerExchange): Unit =
      exchange.getResponseSender.send(value.toJson)
    override def headers: Seq[(String, Seq[String])] = Seq(
      Headers.CONTENT_TYPE_STRING -> Seq("application/json")
    )
  }
  
}

