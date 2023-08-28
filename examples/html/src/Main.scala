package demo

import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.handlers.*
import io.undertow.Undertow
import ba.sake.hepek.html.HtmlPage
import scalatags.Text.all._

@main def main: Unit = {

  val routes: Routes = {
    case GET() -> Path("images", imageName) =>
      val resource = Resource.fromClassPath(s"static/images/$imageName")
      Response.withBodyOpt(resource, "NotFound")

    case GET() -> Path() =>
      Response.withBody(MyPage)
  }

  val server = Undertow
    .builder()
    .addHttpListener(8181, "localhost")
    .setHandler(ErrorHandler(RoutesHandler(routes)))
    .build()

  server.start()

  val serverInfo = server.getListenerInfo().get(0)
  val url = s"${serverInfo.getProtcol}:/${serverInfo.getAddress}"
  println(s"Started HTTP server at $url")

}

val MyPage = new HtmlPage {
  override def bodyContent: Frag = div(
    "Hello sharaf!",
    img(src := "images/scala.png")
  )
}
