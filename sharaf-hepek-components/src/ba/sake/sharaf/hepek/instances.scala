package ba.sake.sharaf.hepek

import java.io.OutputStream
import sttp.model.HeaderNames
import ba.sake.hepek.html.HtmlPage
import ba.sake.sharaf.*

private val ContentTypeHttpString = HttpString(HeaderNames.ContentType)

given ResponseWritable[HtmlPage] with {
  override def write(value: HtmlPage, outputStream: OutputStream): Unit =
    val htmlText = "<!DOCTYPE html>" + value.contents
    ResponseWritable[String].write(htmlText, outputStream)
  override def headers(value: HtmlPage): Seq[(HttpString, Seq[String])] = Seq(
    ContentTypeHttpString -> Seq("text/html; charset=utf-8")
  )
}