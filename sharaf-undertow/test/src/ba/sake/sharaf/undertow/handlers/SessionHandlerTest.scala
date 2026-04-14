package ba.sake.sharaf.undertow.handlers

import ba.sake.sharaf.handlers.AbstractSessionHandlerTest
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.*

class SessionHandlerTest extends AbstractSessionHandlerTest {

  val server = UndertowSharafServer(
    "localhost",
    port,
    SharafHandler.sessions(SharafHandler.routes(routes))
  )

  def startServer(): Unit = server.start()
  def stopServer(): Unit = server.stop()
}
