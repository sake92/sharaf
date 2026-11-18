//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.23.0
//> using dep com.lihaoyi::scalatags:0.13.1

import scalatags.Text.all.*
import ba.sake.sharaf.*
import java.net.InetSocketAddress
import ba.sake.potateh.ServerConfig
import ba.sake.sharaf.potateh.PotatehSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case GET -> Path("hello", name) =>
    Response.withBody(HelloView(name))

PotatehSharafServer.start(ServerConfig.Default.withBindAddress(new InetSocketAddress("localhost", 8181)), routes)

println(s"Server started at http://localhost:8181")

def IndexView = doctype("html")(
  html(
    p("Welcome!"),
    a(href := "/hello/Bob")("Go to /hello/Bob")
  )
)

def HelloView(name: String) = doctype("html")(
  html(
    p("Welcome!"),
    div("Hello ", b(name), "!")
  )
)
