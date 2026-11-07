package ba.sake.sharaf.potateh

import java.net.{URI, URLDecoder}
import java.nio.charset.StandardCharsets.UTF_8
import scala.collection.immutable.SeqMap
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.SharafException

final class PotatehSharafRequest(val underlying: ba.sake.potateh.Request) extends Request {

  private[potateh] lazy val uri: URI = URI.create(underlying.target)

  override lazy val headers: Map[HttpString, Seq[String]] =
    underlying.headers.fields.groupMap { case (name, _) => HttpString(name) } { case (_, value) => value }

  override lazy val cookies: Seq[Cookie] =
    underlying.headers.all("Cookie").flatMap { header =>
      header.split(';').filter(_.trim.nonEmpty).map { item =>
        val parts = item.trim.split("=", 2)
        val value = if parts.length == 2 then parts(1).trim else ""
        Cookie(parts(0).trim, value)
      }
    }

  override lazy val queryParamsRaw: QueryStringMap =
    decodePairs(Option(uri.getRawQuery).getOrElse("")).groupMap(_._1)(_._2)

  override lazy val bodyString: String =
    new String(underlying.body.readAllBytes(), UTF_8)

  override lazy val bodyFormRaw: FormDataMap = {
    val contentType = underlying.headers.first("Content-Type").getOrElse("").takeWhile(_ != ';').trim
    if !contentType.equalsIgnoreCase("application/x-www-form-urlencoded") then
      throw SharafException(s"Unsupported form content type in sharaf-potateh PoC: $contentType")
    decodePairs(bodyString).foldLeft(SeqMap.empty[String, Seq[FormValue]]) { case (values, (key, value)) =>
      values.updated(key, values.getOrElse(key, Seq.empty) :+ FormValue.Str(value))
    }
  }

  private def decodePairs(value: String): Seq[(String, String)] =
    value.split('&').toSeq.filter(_.nonEmpty).map { item =>
      val parts = item.split("=", 2)
      val key = URLDecoder.decode(parts(0), "UTF-8")
      val decoded = if parts.length == 2 then URLDecoder.decode(parts(1), "UTF-8") else ""
      key -> decoded
    }
}
