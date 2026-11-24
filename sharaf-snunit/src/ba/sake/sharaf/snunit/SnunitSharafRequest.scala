package ba.sake.sharaf.snunit

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets
import snunit.{Request as SnunitRequest, *}
import ba.sake.querson.*
import ba.sake.sharaf.*

class SnunitSharafRequest(underlyingRequest: SnunitRequest) extends MultipartRequest {

  def headers: Map[HttpString, Seq[String]] =
    val underlyingHeaders = underlyingRequest.headers
    underlyingHeaders.toMap
      .map { (headerName, headerValue) =>
        HttpString(headerName) -> Seq(headerValue)
      }

  override def cookies: Seq[Cookie] =
    val cookieHeaders = Seq.newBuilder[String]
    underlyingRequest.headers.foreach { (name, value) =>
      if name.equalsIgnoreCase("Cookie") then cookieHeaders += value
    }
    CookieHeaders.parse(cookieHeaders.result())

  override lazy val queryParamsRaw: QueryStringMap =
    UrlEncoded.query(underlyingRequest.query)

  private lazy val bodyBytes = underlyingRequest.contentRaw()

  override lazy val bodyInputStream: InputStream = new ByteArrayInputStream(bodyBytes)

  override lazy val bodyString: String =
    String(bodyBytes, StandardCharsets.UTF_8)

}

object SnunitSharafRequest {

  def create(underlyingRequest: SnunitRequest): SnunitSharafRequest =
    SnunitSharafRequest(underlyingRequest)
}
