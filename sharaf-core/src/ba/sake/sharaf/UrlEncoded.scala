package ba.sake.sharaf

import java.net.URLDecoder
import java.io.{ByteArrayOutputStream, InputStream}
import java.nio.charset.StandardCharsets.{ISO_8859_1, UTF_8}
import scala.collection.immutable.SeqMap
import ba.sake.formson.*
import ba.sake.querson.QueryStringMap
import ba.sake.sharaf.exceptions.{BadRequestException, PayloadTooLargeException}

/** Internal parsing of raw query strings and URL-encoded forms, never paths or whole URLs. */
private[sharaf] object UrlEncoded {
  val maxFormBytes: Int = 1024 * 1024

  def isForm(contentType: String): Boolean =
    contentType.takeWhile(_ != ';').trim.equalsIgnoreCase("application/x-www-form-urlencoded")

  def readFormBody(input: InputStream): String = {
    val output = new ByteArrayOutputStream()
    val buffer = new Array[Byte](8192)
    var done = false
    while !done do {
      val allowed = math.min(buffer.length, maxFormBytes - output.size() + 1)
      val count = input.read(buffer, 0, allowed)
      if count < 0 then done = true
      else if count > 0 then {
        if count > maxFormBytes - output.size() then throw PayloadTooLargeException("URL-encoded form limit exceeded")
        output.write(buffer, 0, count)
      }
    }
    new String(output.toByteArray, UTF_8)
  }

  def query(raw: String): QueryStringMap = {
    val values = scala.collection.mutable.Map.empty[String, scala.collection.mutable.ArrayBuffer[String]]
    var start = 0
    while (start < raw.length) {
      val separator = raw.indexOf('&', start)
      val end = if (separator < 0) raw.length else separator
      if (end > start) {
        var split = start
        while (split < end && raw.charAt(split) != '=') split += 1
        val name = component(raw.substring(start, split))
        val value = if (split == end) "" else component(raw.substring(split + 1, end))
        values.getOrElseUpdate(name, scala.collection.mutable.ArrayBuffer.empty[String]) += value
      }
      start = end + 1
    }
    values.iterator.map { (key, entries) => key -> entries.toVector }.toMap
  }

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
    if (!value.contains('%') && !value.contains('+')) value
    else
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
