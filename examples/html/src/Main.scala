package demo

import ba.sake.validation.*
import ba.sake.formson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.handlers.*
import io.undertow.Undertow
import ba.sake.hepek.html.HtmlPage
import scalatags.Text.all._

@main def main: Unit = {

  val routes: Routes = { case (GET(), Path("html"), _) =>
    Response.withBody(MyPage: HtmlPage)
  }

  val server = Undertow
    .builder()
    .addHttpListener(8181, "localhost")
    .setHandler(
      ErrorHandler(
        SharafHandler(routes)
      )
    )
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
