import ba.sake.sharaf.*
import ba.sake.sharaf.snunit.*

@main def main: Unit =

  val routes = Routes { case _ =>
    Response.withBody("Hello Snunit!")
  }
  val server = _root_.snunit.SyncServerBuilder
    .setRequestHandler(SharafRequestHandler(routes))
    .build()

  server.listen()
