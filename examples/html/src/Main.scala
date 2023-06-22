package demo

import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.handlers.*
import io.undertow.Undertow
import ba.sake.hepek.html.HtmlPage
import scalatags.Text.all._

@main def main: Unit = {

  val routes: Routes = {
    case GET() -> Path("html") =>
      val htmlPage: HtmlPage = MyPage
      Response.withBody(htmlPage)
    case GET() -> Path("scala.png") =>
      val resource = Resource.fromClassPath("static/scala.png")
      Response.withBodyOpt(resource, "NotFound")
  }

  val server = Undertow
    .builder()
    .addHttpListener(8181, "localhost")
    .setHandler(RoutesHandler(routes))
    .build()

  server.start()

  val serverInfo = server.getListenerInfo().get(0)
  val url = s"${serverInfo.getProtcol}:/${serverInfo.getAddress}"
  println(s"Started HTTP server at $url")

}

object MyPage extends HtmlPage {
  override def bodyContent: Frag = div(
    "oppppppp",
    img(src := "scala.png")
  )
}
