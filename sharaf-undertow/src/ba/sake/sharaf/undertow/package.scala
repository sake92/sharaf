package ba.sake.sharaf.undertow

import java.io.OutputStream
import ba.sake.hepek.html.HtmlPage
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*


type UndertowSharafRoutes = SharafRoutes[UndertowSharafRequest]
type UndertowSharafController = SharafController[UndertowSharafRequest]

object UndertowSharafRoutes:
  def apply(routesDef: UndertowSharafRequest ?=> PartialFunction[RequestParams, Response[?]]): UndertowSharafRoutes =
    SharafRoutes(routesDef)
  export SharafRoutes.merge

// TODO separate library
given ResponseWritable[HtmlPage] with {
  override def write(value: HtmlPage, outputStream: OutputStream): Unit =
    val htmlText = "<!DOCTYPE html>" + value.contents
    ResponseWritable[String].write(htmlText, outputStream)
  override def headers(value: HtmlPage): Seq[(HttpString, Seq[String])] = Seq(
    Headers.CONTENT_TYPE -> Seq("text/html; charset=utf-8")
  )
}

given (using r: UndertowSharafRequest): Session =
  val s = io.undertow.util.Sessions.getOrCreateSession(r.underlyingHttpServerExchange)
  UndertowSharafSession(s)
