package ba.sake.sharaf.helidon

import java.nio.charset.StandardCharsets
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

  def cookies: Seq[Cookie] = ??? // TODO
  // underlyingHttpServerExchange.requestCookies().asScala.map(CookieUtils.fromUndertow).toSeq

  /* *** QUERY *** */
  override lazy val queryParamsRaw: QueryStringMap =
    underlyingRequest.query().toMap.asScala.toMap.map { (k, v) =>
      (k, v.asScala.toSeq)
    }

  /* *** BODY *** */
  override lazy val bodyString: String =
    String(underlyingRequest.content().inputStream().readAllBytes(), StandardCharsets.UTF_8)

  def bodyFormRaw: FormDataMap = ??? // TODO
}

object HelidonSharafRequest {

  def create(underlyingRequest: ServerRequest): HelidonSharafRequest =
    HelidonSharafRequest(underlyingRequest)
}
