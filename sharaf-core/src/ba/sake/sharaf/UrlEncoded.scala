package ba.sake.sharaf

import java.net.URLDecoder
import java.nio.charset.StandardCharsets.{ISO_8859_1, UTF_8}
import scala.collection.immutable.SeqMap
import ba.sake.formson.*
import ba.sake.querson.QueryStringMap
import ba.sake.sharaf.exceptions.BadRequestException

/** Internal parsing of raw query strings and URL-encoded forms, never paths or whole URLs. */
private[sharaf] object UrlEncoded {
  def query(raw: String): QueryStringMap =
    pairs(raw).groupMap(_.name)(_.value)

  def form(raw: String): FormDataMap =
    pairs(raw).foldLeft(SeqMap.empty[String, Seq[FormValue]]) { (values, pair) =>
      values.updated(pair.name, values.getOrElse(pair.name, Seq.empty) :+ FormValue.Str(pair.value))
    }

  private def pairs(raw: String): Seq[(name: String, value: String)] =
    raw.split('&').toSeq.filter(_.nonEmpty).map { item =>
      val parts = item.split("=", 2)
      val name = component(parts(0))
      val value = if parts.length == 2 then component(parts(1)) else ""
      (name = name, value = value)
    }

  private def component(value: String): String =
    try URLDecoder.decode(value, "UTF-8")
    catch
      case _: IllegalArgumentException =>
        // Scala Native 0.5.12 reports malformed UTF-8 as an illegal escape instead of replacing it.
        // Decode each escaped byte run through Latin-1, then apply UTF-8 replacement explicitly.
        try componentWithReplacement(value)
        catch case _: IllegalArgumentException => throw BadRequestException("Invalid percent escape in request")

  private def componentWithReplacement(value: String): String = {
    val result = new StringBuilder()
    var index = 0
    while index < value.length do {
      if value.charAt(index) == '%' then {
        val start = index
        while index < value.length && value.charAt(index) == '%' do index = math.min(index + 3, value.length)
        val escaped = value.substring(start, index)
        val bytes = URLDecoder.decode(escaped, "ISO-8859-1").getBytes(ISO_8859_1)
        result.append(new String(bytes, UTF_8))
      } else {
        val char = value.charAt(index)
        result.append(if char == '+' then ' ' else char)
        index += 1
      }
    }
    result.toString
  }
}
