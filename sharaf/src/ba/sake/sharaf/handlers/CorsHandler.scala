package ba.sake.sharaf.handlers

import java.time.Duration
import scala.jdk.CollectionConverters.*
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import io.undertow.util.HttpString
import io.undertow.util.Methods

import ba.sake.sharaf.*

final class CorsHandler private (corsSettings: CorsSettings) extends HttpHandler {

  private val accessControlAllowOrigin = new HttpString("Access-Control-Allow-Origin")

  private val accessControlAllowMethods = new HttpString("Access-Control-Allow-Methods")
  private val acccessControlAllowHeaders = new HttpString("Access-Control-Allow-Headers")
  private val accessControlAllowCredentials = new HttpString("Access-Control-Allow-Credentials")

  override def handleRequest(exchange: HttpServerExchange): Unit = {
    exchange.startBlocking()
    if exchange.isInIoThread then {
      exchange.dispatch(this)
    } else if corsSettings.pathPrefixes.contains(exchange.getRequestPath) then {
      if exchange.getRequestMethod() == Methods.OPTIONS then {
        exchange
          .getResponseHeaders()
          .putAll(accessControlAllowMethods, corsSettings.allowedHttpMethods.map(_.toString).asJava)
        exchange
          .getResponseHeaders()
          .putAll(acccessControlAllowHeaders, corsSettings.allowedHttpHeaders.map(_.toString).asJava)
        exchange
          .getResponseHeaders()
          .put(accessControlAllowCredentials, corsSettings.allowCredentials.toString)
      } else {
        Option(exchange.getRequestHeaders.getFirst(Headers.ORIGIN)) match {
          case None => // noop
          case Some(origin) =>
            if corsSettings.allowedOrigins.contains(origin) then
              exchange.getResponseHeaders().put(accessControlAllowOrigin, origin)
        }
      }
    }
  }
}

// stolen from Play
// https://www.playframework.com/documentation/2.8.x/CorsFilter#Configuring-the-CORS-filter
case class CorsSettings(
    pathPrefixes: Set[String] = Set("/"),
    allowedOrigins: Set[String] = Set("*"),
    allowedHttpMethods: Set[HttpString] =
      Set(Methods.GET, Methods.HEAD, Methods.OPTIONS, Methods.POST, Methods.PUT, Methods.PATCH, Methods.DELETE),
    allowedHttpHeaders: Set[HttpString] = Set(Headers.ACCEPT),
    allowCredentials: Boolean = true,
    preflightMaxAge: Duration = Duration.ofDays(3)
)
