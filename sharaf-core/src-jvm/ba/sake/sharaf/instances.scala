package ba.sake.sharaf

import java.io.OutputStream
import play.twirl.api.{Html, Xml}

// TODO move to common when published for native
export play.twirl.api.StringInterpolation

export play.twirl.api.Html
export play.twirl.api.Xml

// twirl HTML and XML
given ResponseWritable[Html] with {
  override def write(value: Html, outputStream: OutputStream): Unit =
    ResponseWritable[String].write(value.toString, outputStream)
  override def headers(value: Html): Seq[(HttpString, Seq[String])] = Seq(
    ContentTypeHttpString -> Seq("text/html; charset=utf-8")
  )
}

given ResponseWritable[Xml] with {
  override def write(value: Xml, outputStream: OutputStream): Unit =
    ResponseWritable[String].write(value.toString, outputStream)
  override def headers(value: Xml): Seq[(HttpString, Seq[String])] = Seq(
    ContentTypeHttpString -> Seq("application/xml; charset=utf-8")
  )
}
