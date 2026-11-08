package ba.sake.sharaf.potateh

import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import scala.collection.immutable.SeqMap
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.Path
import ba.sake.sharaf.exceptions.{BadRequestException, UnsupportedMediaTypeException}

final class PotatehSharafRequest(val underlying: ba.sake.potateh.Request) extends Request {

  private[potateh] lazy val uri: URI =
    try {
      val target = underlying.target
      val absolute = if target.startsWith("/") then "http://request.invalid" + target else target
      URI.create(absolute)
    } catch case _: IllegalArgumentException => throw BadRequestException("Invalid request target")

  private[potateh] def path: Path = {
    val raw = Option(uri.getRawPath).getOrElse("")
    val segments = raw.stripPrefix("/").split("/")
    if segments.sameElements(Array("")) then Path()
    else Path(segments.map(segment => RequestDecoding.component(segment, plusAsSpace = false))*)
  }

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

  private lazy val bodyBytes: Array[Byte] = underlying.body.readAllBytes()

  override lazy val bodyString: String = new String(bodyBytes, UTF_8)

  override lazy val bodyFormRaw: FormDataMap = {
    val contentType = underlying.headers.first("Content-Type").getOrElse("").takeWhile(_ != ';').trim
    if !contentType.equalsIgnoreCase("application/x-www-form-urlencoded") then
      throw UnsupportedMediaTypeException("Expected application/x-www-form-urlencoded")
    decodePairs(RequestDecoding.utf8(bodyBytes)).foldLeft(SeqMap.empty[String, Seq[FormValue]]) {
      case (values, (key, value)) =>
        values.updated(key, values.getOrElse(key, Seq.empty) :+ FormValue.Str(value))
    }
  }

  private def decodePairs(value: String): Seq[(String, String)] =
    value.split('&').toSeq.filter(_.nonEmpty).map { item =>
      val parts = item.split("=", 2)
      val key = RequestDecoding.component(parts(0), plusAsSpace = true)
      val decoded = if parts.length == 2 then RequestDecoding.component(parts(1), plusAsSpace = true) else ""
      key -> decoded
    }
}
