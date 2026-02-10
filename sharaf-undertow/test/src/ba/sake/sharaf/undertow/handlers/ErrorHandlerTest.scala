package ba.sake.sharaf.undertow.handlers

import io.undertow.{Handlers, Undertow}
import io.undertow.server.handlers.BlockingHandler
import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractErrorHandlerTest
import ba.sake.sharaf.undertow.SharafUndertowHandler

class ErrorHandlerTest extends AbstractErrorHandlerTest {
  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(
      Handlers
        .path()
        .addPrefixPath(
          "default",
          BlockingHandler(
            SharafUndertowHandler(
              SharafHandler.exceptions(SharafHandler.routes(routes))
            )
          )
        )
        .addPrefixPath(
          "json",
          BlockingHandler(
            SharafUndertowHandler(
              SharafHandler.exceptions(
                SharafHandler.routes(routes),
                ExceptionMapper.json
              )
            )
          )
        )
    )
    .build()

  def startServer(): Unit = server.start()
  def stopServer(): Unit = server.stop()
}
