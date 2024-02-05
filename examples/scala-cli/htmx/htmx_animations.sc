//> using scala "3.3.1"
//> using dep ba.sake::sharaf:0.0.22

// https://htmx.org/examples/animations/

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.html.HtmlPage
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*, routing.*

trait ExamplePage extends HtmlPage with HtmxDependencies

object IndexView extends ExamplePage:
  override def bodyContent = ul(
    li(a(href := "color-throb")("Color throb")),
    li(a(href := "fade-out-on-swap")("Fade Out On Swap")),
    li(a(href := "fade-in-on-addition")("Fade In On Addition"))
  )

object ColorThrobView extends ExamplePage:
  override def bodyContent = snippet("red")

  def snippet(color: String) = div(
    id := "color-demo", // must stay same!
    hx.get := "/colors",
    hx.swap := "outerHTML",
    hx.trigger := "every 1s",
    cls := "smooth",
    style := s"color:${color}"
  )("Color Swap Demo")

  override def stylesInline = List("""
    .smooth {
      transition: all 1s ease-in;
    }
  """)

object FadeOutOnSwapView extends ExamplePage:
  override def bodyContent = button(
    cls := "fade-me-out",
    hx.delete := "/fade_out_demo",
    hx.swap := "outerHTML swap:1s"
  )("Fade Me Out")

  override def stylesInline = List("""
    .fade-me-out.htmx-swapping {
      opacity: 0;
      transition: opacity 1s ease-out;
    }
  """)

object FadeInOnAdditionView extends ExamplePage:
  override def bodyContent = theButton

  val theButton = button(
    id := "fade-me-in",
    hx.post := "/fade_in_demo",
    hx.swap := "outerHTML settle:1s"
  )("Fade Me In")

  override def stylesInline = List("""
    #fade-me-in.htmx-added {
      opacity: 0;
    }
    #fade-me-in {
      opacity: 1;
      transition: opacity 1s ease-out;
    }
  """)

val routes = Routes:
  case GET() -> Path() =>
    Response.withBody(IndexView)

  case GET() -> Path("color-throb") =>
    Response.withBody(ColorThrobView)
  case GET() -> Path("colors") =>
    // generate a random #aBc color
    // https://stackoverflow.com/a/19298151
    val x = scala.util.Random.nextInt(256)
    val randomColor = String.format("#%03X", x)
    Response.withBody(ColorThrobView.snippet(randomColor))

  case GET() -> Path("fade-out-on-swap") =>
    Response.withBody(FadeOutOnSwapView)
  case DELETE() -> Path("fade_out_demo") =>
    Response.withBody("")

  case GET() -> Path("fade-in-on-addition") =>
    Response.withBody(FadeInOnAdditionView)
  case POST() -> Path("fade_in_demo") =>
    Response.withBody(FadeInOnAdditionView.theButton)

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
