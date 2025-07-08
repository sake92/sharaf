import ba.sake.sharaf.*
import ba.sake.sharaf.snunit.*

@main def main: Unit =

  val routes = Routes {
    case GET -> Path("hello", name) =>
      Response.withBody(s"Hello ${name}!")
    case _ =>
      Response.withBody("Hello Snunit!")
  }
  val server = _root_.snunit.SyncServerBuilder
    .setRequestHandler(
      SharafRequestHandler(SharafHandler.routes(routes))
    )
    .build()
  server.listen()
