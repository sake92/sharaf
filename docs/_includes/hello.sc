//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.24.0

import ba.sake.sharaf.*
import ba.sake.sharaf.potateh.PotatehSharafServer

val routes = Routes:
  case GET -> Path("hello", name) =>
    Response.withBody(s"Hello $name")
  case GET -> _ =>
    Response.withBody("Go to http://localhost:8181/hello/your-name")

PotatehSharafServer(routes, host = "localhost", port = 8181).start()

println(s"Server started at http://localhost:8181")
