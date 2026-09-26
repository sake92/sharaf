package ba.sake.sharaf.helidon

import java.nio.charset.StandardCharsets
import java.net.URLDecoder
import scala.collection.immutable.SeqMap
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.jdk.StreamConverters.*
import io.helidon.webserver.http.ServerRequest
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.*

class HelidonSharafRequest(underlyingRequest: ServerRequest) extends Request {

  /* *** HEADERS *** */
  def headers: Map[HttpString, Seq[String]] =
    val underlyingHeaders = underlyingRequest.headers()
    underlyingHeaders.stream
      .toScala(LazyList)
      .map { header =>
        HttpString(header.name()) -> header.values().split(",").toSeq
      }
      .toMap

  def cookies: Seq[Cookie] =
    headers.get(HttpString("Cookie")).toSeq.flatten.flatMap { header =>
      header.split(";").toSeq.map(_.trim).filter(_.nonEmpty).map { item =>
        val parts = item.split("=", 2)
        Cookie(parts(0).trim, if parts.length == 2 then parts(1).trim else "")
      }
    }

  /* *** QUERY *** */
  override lazy val queryParamsRaw: QueryStringMap =
    underlyingRequest.query().toMap.asScala.toMap.map { (k, v) =>
      (k, v.asScala.toSeq)
    }

  /* *** BODY *** */
  override lazy val bodyString: String =
    String(underlyingRequest.content().inputStream().readAllBytes(), StandardCharsets.UTF_8)

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
    else throw SharafException(s"Unsupported content type for form data in sharaf-helidon: $contentType")
}

object HelidonSharafRequest {

  def create(underlyingRequest: ServerRequest): HelidonSharafRequest =
    HelidonSharafRequest(underlyingRequest)
}
