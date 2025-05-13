package ba.sake.sharaf.undertow

import java.io.OutputStream
import ba.sake.hepek.html.HtmlPage
import ba.sake.sharaf.*
import sttp.model.HeaderNames

// TODO separate library
given ResponseWritable[HtmlPage] with {
  override def write(value: HtmlPage, outputStream: OutputStream): Unit =
    val htmlText = "<!DOCTYPE html>" + value.contents
    ResponseWritable[String].write(htmlText, outputStream)
  override def headers(value: HtmlPage): Seq[(HttpString, Seq[String])] = Seq(
    HttpString(HeaderNames.ContentType) -> Seq("text/html; charset=utf-8")
  )
}

given (using r: Request): Session =
  val undertowReq = r.asInstanceOf[UndertowSharafRequest]
  val s = io.undertow.util.Sessions.getOrCreateSession(undertowReq.underlyingHttpServerExchange)
  UndertowSharafSession(s)
