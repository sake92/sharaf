//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.23.0

import ba.sake.sharaf.*
import java.net.InetSocketAddress
import ba.sake.potateh.ServerConfig
import ba.sake.sharaf.potateh.PotatehSharafServer

val routes = Routes:
  case GET -> Path("hello", name) =>
    Response.withBody(s"Hello $name")
  case GET -> _ =>
    Response.withBody("Go to http://localhost:8181/hello/your-name")

PotatehSharafServer.start(ServerConfig.Default.withBindAddress(new InetSocketAddress("localhost", 8181)), routes)

println(s"Server started at http://localhost:8181")
