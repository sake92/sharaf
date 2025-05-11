package ba.sake.sharaf.handlers

import ba.sake.sharaf.*
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.{Headers, HttpString, Methods}

import scala.jdk.CollectionConverters.*

// TODO write some tests
// https://www.moesif.com/blog/technical/cors/Authoritative-Guide-to-CORS-Cross-Origin-Resource-Sharing-for-REST-APIs/
final class CorsHandler private (next: HttpHandler, corsSettings: CorsSettings) extends HttpHandler {

  private val accessControlAllowOrigin = HttpString("Access-Control-Allow-Origin")
  private val accessControlAllowCredentials = HttpString("Access-Control-Allow-Credentials")

  // only for OPTIONS / preflight
  private val accessControlAllowMethods = HttpString("Access-Control-Allow-Methods")
  private val accessControlAllowHeaders = HttpString("Access-Control-Allow-Headers")

  override def handleRequest(exchange: HttpServerExchange): Unit =
    if exchange.getRequestMethod == Methods.OPTIONS then
      setCorsHeaders(exchange)
      setPreflightHeaders(exchange)
    else
      setCorsHeaders(exchange)
      next.handleRequest(exchange)

  private def setPreflightHeaders(exchange: HttpServerExchange): Unit = {
    exchange.getResponseHeaders
      .putAll(accessControlAllowMethods, corsSettings.allowedHttpMethods.map(_.toString).asJava)
    exchange.getResponseHeaders
      .putAll(accessControlAllowHeaders, corsSettings.allowedHttpHeaders.map(_.toString).asJava)
  }

  private def setCorsHeaders(exchange: HttpServerExchange): Unit = {
    exchange.getResponseHeaders
      .put(accessControlAllowCredentials, corsSettings.allowCredentials.toString)
    if corsSettings.allowedOrigins.contains("*") then exchange.getResponseHeaders.put(accessControlAllowOrigin, "*")
    else
      Option(exchange.getRequestHeaders.getFirst(Headers.ORIGIN)) match {
        case None => // noop
        case Some(origin) =>
          if corsSettings.allowedOrigins.contains(origin) then
            exchange.getResponseHeaders.put(accessControlAllowOrigin, origin)
      }
  }
}

object CorsHandler:
  def apply(next: HttpHandler, corsSettings: CorsSettings): CorsHandler =
    new CorsHandler(next, corsSettings)
