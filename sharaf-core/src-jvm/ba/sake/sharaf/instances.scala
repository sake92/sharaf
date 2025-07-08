package ba.sake.sharaf

import java.io.OutputStream
import play.twirl.api.{Html, Xml}

object ResponseWritableInstances {
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

}
