import ba.sake.sharaf.*
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer

@main def main: Unit =
  val routes = Routes:
    case GET -> Path("hello", name) =>
      Response.withBody(s"Hello $name")

  JdkHttpServerSharafServer("localhost", 8181, routes).start()

  println("Server started at http://localhost:8181")
  println("Try: curl http://localhost:8181/hello/world")
