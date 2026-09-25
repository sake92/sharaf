package ba.sake.sharaf.snunit

import java.nio.charset.StandardCharsets
import java.net.URLDecoder
import scala.collection.immutable.SeqMap
import scala.collection.mutable
import snunit.{Request as SnunitRequest, *}
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.*

class SnunitSharafRequest(underlyingRequest: SnunitRequest) extends Request {

  /* *** HEADERS *** */
  def headers: Map[HttpString, Seq[String]] =
    val underlyingHeaders = underlyingRequest.headers
    underlyingHeaders.toMap
      .map { (headerName, headerValue) =>
        HttpString(headerName) -> Seq(headerValue)
      }

  def cookies: Seq[Cookie] =
    val builder = Seq.newBuilder[Cookie]
    val underlyingHeaders = underlyingRequest.headers
    // TODO: Use underlyingRequest.cookieFieldIndex when available
    underlyingHeaders.foreach {
      case (name, cookieString) if name.equalsIgnoreCase("Cookie") =>
        cookieString.split(';').foreach {
          case item if item.trim.nonEmpty =>
            val parts = item.trim.split("=", 2)
            builder += Cookie(name = parts(0).trim, value = if parts.length == 2 then parts(1).trim else "")
          case _ => ()
        }
      case _ =>
    }
    builder.result()

  /* *** QUERY *** */
  override lazy val queryParamsRaw: QueryStringMap =
    underlyingRequest.query
      .split("&")
      .flatMap(_.split("=") match {
        case Array(key, value) => Seq(key -> Seq(value))
        case _                 => Seq.empty
      })
      .toMap

  /* *** BODY *** */
  override lazy val bodyString: String =
    String(underlyingRequest.contentRaw(), StandardCharsets.UTF_8)

  override lazy val bodyFormRaw: FormDataMap =
    val contentType = headers.get(HttpString("Content-Type")).flatMap(_.headOption).getOrElse("")
    if contentType.startsWith("application/x-www-form-urlencoded") then
      val values = mutable.LinkedHashMap.empty[String, Seq[FormValue]]
      bodyString.split("&").filter(_.nonEmpty).foreach { item =>
        val parts = item.split("=", 2)
        val key = URLDecoder.decode(parts(0), StandardCharsets.UTF_8)
        val value = if parts.length == 2 then URLDecoder.decode(parts(1), StandardCharsets.UTF_8) else ""
        values.updateWith(key) {
          case Some(existing) => Some(existing :+ FormValue.Str(value))
          case None           => Some(Seq(FormValue.Str(value)))
        }
      }
      SeqMap.from(values)
    else throw SharafException(s"Unsupported content type for form data in sharaf-snunit: $contentType")
}

object SnunitSharafRequest {

  def create(underlyingRequest: SnunitRequest): SnunitSharafRequest =
    SnunitSharafRequest(underlyingRequest)
}
