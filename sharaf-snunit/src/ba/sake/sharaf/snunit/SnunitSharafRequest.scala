package ba.sake.sharaf.snunit

import java.nio.charset.StandardCharsets
import snunit.{Request as SnunitRequest, *}
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*

class SnunitSharafRequest(underlyingRequest: SnunitRequest) extends Request {

  /* *** HEADERS *** */
  def headers: Map[HttpString, Seq[String]] =
    val underlyingHeaders = underlyingRequest.headers
    underlyingHeaders.toMap
      .map { (headerName, headerValue) =>
        HttpString(headerName) -> Seq(headerValue)
      }

  def cookies: Seq[Cookie] = ??? // TODO
  // underlyingHttpServerExchange.requestCookies().asScala.map(CookieUtils.fromUndertow).toSeq

  /* *** QUERY *** */
  override lazy val queryParamsRaw: QueryStringMap =
    underlyingRequest.query.split("&").flatMap( _.split("=") match {
      case Array(key, value) => Seq(key -> Seq(value))
      case _        => Seq.empty
    })
    .toMap

  /* *** BODY *** */
  override lazy val bodyString: String =
    String(underlyingRequest.contentRaw(), StandardCharsets.UTF_8)

  def bodyFormRaw: FormDataMap = ??? // TODO
}

object SnunitSharafRequest {

  def create(underlyingRequest: SnunitRequest): SnunitSharafRequest =
    SnunitSharafRequest(underlyingRequest)
}
