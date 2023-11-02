package ba.sake.sharaf.handlers

import java.time.Duration
import scala.jdk.CollectionConverters.*
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import io.undertow.util.HttpString
import io.undertow.util.Methods

import ba.sake.sharaf.*

// TODO write some tests
final class CorsHandler private (next: HttpHandler, corsSettings: CorsSettings) extends HttpHandler {

  private val accessControlAllowOrigin = HttpString("Access-Control-Allow-Origin")
  private val accessControlAllowCredentials = HttpString("Access-Control-Allow-Credentials")

  // only for OPTIONS / preflight
  private val accessControlAllowMethods = HttpString("Access-Control-Allow-Methods")
  private val acccessControlAllowHeaders = HttpString("Access-Control-Allow-Headers")

  override def handleRequest(exchange: HttpServerExchange): Unit = {
    exchange.startBlocking()
    if exchange.isInIoThread then exchange.dispatch(this)
    else {
      if exchange.getRequestMethod() == Methods.OPTIONS then
        setCorsHeaders(exchange)
        setPreflightHeaders(exchange)
      else
        setCorsHeaders(exchange)
        next.handleRequest(exchange)
    }
  }

  private def setPreflightHeaders(exchange: HttpServerExchange): Unit = {
    exchange
      .getResponseHeaders()
      .putAll(accessControlAllowMethods, corsSettings.allowedHttpMethods.map(_.toString).asJava)
    exchange
      .getResponseHeaders()
      .putAll(acccessControlAllowHeaders, corsSettings.allowedHttpHeaders.map(_.toString).asJava)
  }

  private def setCorsHeaders(exchange: HttpServerExchange): Unit = {

    exchange
      .getResponseHeaders()
      .put(accessControlAllowCredentials, corsSettings.allowCredentials.toString)

    if corsSettings.allowedOrigins.contains("*") then exchange.getResponseHeaders().put(accessControlAllowOrigin, "*")
    else
      Option(exchange.getRequestHeaders.getFirst(Headers.ORIGIN)) match {
        case None => // noop
        case Some(origin) =>
          if corsSettings.allowedOrigins.contains(origin) then
            exchange.getResponseHeaders().put(accessControlAllowOrigin, origin)
      }

  }
}

object CorsHandler {
  def apply(next: HttpHandler, corsSettings: CorsSettings): CorsHandler = {
    new CorsHandler(next, corsSettings)
  }
}

// stolen from Play
// https://www.playframework.com/documentation/2.8.x/CorsFilter#Configuring-the-CORS-filter
// https://developer.mozilla.org/en-US/docs/Glossary/CORS-safelisted_request_header
case class CorsSettings(
    pathPrefixes: Set[String] = Set("/"),
    allowedOrigins: Set[String] = Set.empty,
    allowedHttpMethods: Set[HttpString] =
      Set(Methods.GET, Methods.HEAD, Methods.OPTIONS, Methods.POST, Methods.PUT, Methods.PATCH, Methods.DELETE),
    allowedHttpHeaders: Set[HttpString] =
      Set(Headers.ACCEPT, Headers.ACCEPT_LANGUAGE, Headers.CONTENT_LANGUAGE, Headers.CONTENT_TYPE),
    allowCredentials: Boolean = false,
    preflightMaxAge: Duration = Duration.ofDays(3)
)
