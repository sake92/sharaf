package ba.sake.sharaf.jdkhttp

import java.nio.charset.StandardCharsets
import java.net.URLDecoder
import scala.collection.immutable.SeqMap
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import com.sun.net.httpserver.HttpExchange
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.*

final class JdkHttpServerSharafRequest(val underlyingHttpExchange: HttpExchange) extends Request {

  /* *** HEADERS *** */
  def headers: Map[HttpString, Seq[String]] =
    underlyingHttpExchange.getRequestHeaders.asScala.toMap.map { (name, values) =>
      HttpString(name) -> values.asScala.toSeq
    }

  def cookies: Seq[Cookie] =
    headers
      .get(HttpString("Cookie"))
      .toSeq
      .flatten
      .flatMap { cookieHeader =>
        cookieHeader
          .split(";")
          .map(_.trim)
          .map { cookieStr =>
            val parts = cookieStr.split("=", 2)
            if parts.length == 2 then
              Cookie(name = parts(0).trim, value = parts(1).trim)
            else
              Cookie(name = parts(0).trim, value = "")
          }
      }

  /* *** QUERY *** */
  override lazy val queryParamsRaw: QueryStringMap =
    val query = underlyingHttpExchange.getRequestURI.getQuery
    if query == null || query.isEmpty then Map.empty
    else
      query
        .split("&")
        .map { param =>
          val parts = param.split("=", 2)
          val key = URLDecoder.decode(parts(0), StandardCharsets.UTF_8)
          val value =
            if parts.length == 2 then URLDecoder.decode(parts(1), StandardCharsets.UTF_8)
            else ""
          (key, value)
        }
        .groupBy(_._1)
        .map { case (k, v) => (k, v.map(_._2).toSeq) }

  /* *** BODY *** */
  override lazy val bodyString: String =
    val bytes = underlyingHttpExchange.getRequestBody.readAllBytes()
    String(bytes, StandardCharsets.UTF_8)

  override def bodyFormRaw: FormDataMap =
    val contentType = headers.get(HttpString("Content-Type")).flatMap(_.headOption).getOrElse("")

    if contentType.startsWith("application/x-www-form-urlencoded") then
      val formData = bodyString
      if formData.isEmpty then SeqMap.empty
      else
        val map = mutable.LinkedHashMap.empty[String, Seq[FormValue]]
        formData
          .split("&")
          .foreach { param =>
            val parts = param.split("=", 2)
            val key = URLDecoder.decode(parts(0), StandardCharsets.UTF_8)
            val value =
              if parts.length == 2 then URLDecoder.decode(parts(1), StandardCharsets.UTF_8)
              else ""
            val formValue = FormValue.Str(value)
            map.updateWith(key) {
              case Some(existing) => Some(existing :+ formValue)
              case None           => Some(Seq(formValue))
            }
          }
        SeqMap.from(map)
    else if contentType.startsWith("multipart/form-data") then
      // TODO: Implement multipart/form-data parsing for file uploads
      // For now, throw an exception similar to what Helidon does
      throw SharafException(
        "multipart/form-data is not yet supported in sharaf-jdk-httpserver. Use sharaf-undertow for file upload support."
      )
    else
      throw SharafException(s"Unsupported content type for form data: $contentType")

  override def toString(): String =
    s"JdkHttpServerSharafRequest(headers=${headers}, cookies=${cookies}, queryParamsRaw=${queryParamsRaw}, bodyString=...)"
}

object JdkHttpServerSharafRequest {
  def create(httpExchange: HttpExchange): JdkHttpServerSharafRequest =
    JdkHttpServerSharafRequest(httpExchange)
}
