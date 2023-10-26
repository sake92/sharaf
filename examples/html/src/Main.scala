package demo

import io.undertow.Undertow
import ba.sake.sharaf.*, routing.*
import ba.sake.hepek.html.HtmlPage
import scalatags.Text.all._

@main def main: Unit =
  val module = HtmlModule(8181)
  module.server.start()
  println(s"Started HTTP server at ${module.baseUrl}")

class HtmlModule(port: Int) {

  val baseUrl = s"http://localhost:${port}"

  private val routes: Routes =
    case GET() -> Path() =>
      Response.withBody(MyPage)

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(SharafHandler(routes))
    .build()
}

val MyPage = new HtmlPage {
  override def pageContent: Frag = div(
    "Hello sharaf!",
    img(src := "images/scala.png")
  )
}
