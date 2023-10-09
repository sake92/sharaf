package demo

import io.undertow.Undertow
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.handlers.*
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

  val port = 8181

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(ErrorHandler(RoutesHandler(routes)))
    .build()

  server.start()

  println(s"Started HTTP server at http://localhost:${port}")
}

val MyPage = new HtmlPage {
  override def bodyContent: Frag = div(
    "Hello sharaf!",
    img(src := "images/scala.png")
  )
}
