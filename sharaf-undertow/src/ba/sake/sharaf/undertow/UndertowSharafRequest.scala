package ba.sake.sharaf.undertow

import java.io.InputStream
import scala.jdk.CollectionConverters.*
import io.undertow.server.HttpServerExchange
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.*

final class UndertowSharafRequest(val underlyingHttpServerExchange: HttpServerExchange) extends MultipartRequest {

  def headers: Map[HttpString, Seq[String]] =
    val hMap = underlyingHttpServerExchange.getRequestHeaders
    hMap.getHeaderNames.asScala.map { name =>
      HttpString(name.toString) -> hMap.get(name).asScala.toSeq
    }.toMap

  override lazy val queryParamsRaw: QueryStringMap =
    UrlEncoded.query(underlyingHttpServerExchange.getQueryString)

  override def bodyInputStream: InputStream = underlyingHttpServerExchange.getInputStream

  private[sharaf] override def readUrlEncodedBody(input: InputStream): String =
    try super.readUrlEncodedBody(input)
    catch {
      case error: PayloadTooLargeException =>
        underlyingHttpServerExchange.setPersistent(false)
        throw error
    }

  override def toString(): String =
    "UndertowSharafRequest(...)"
}
