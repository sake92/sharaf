package demo

import org.pac4j.core.client.Clients
import ba.sake.sharaf.*
import ba.sake.sharaf.pac4j.*
import ba.sake.sharaf.undertow.UndertowSharafServer

class AppModule(port: Int, clients: Clients) {

  val baseUrl = s"http://localhost:${port}"

  private val securityConfig = SecurityConfig(clients)
  private val appRoutes = AppRoutes()

  val server = UndertowSharafServer(
    "localhost",
    port,
    Pac4jSecurityHandler(securityConfig.securityConfig, SharafHandler.routes(appRoutes.routes))
  )
}
