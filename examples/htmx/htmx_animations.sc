//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

// https://htmx.org/examples/animations/

import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(views.IndexView)

  case GET -> Path("color-throb") =>
    Response.withBody(views.ColorThrobView)
  case GET -> Path("colors") =>
    // generate a random #aBc color
    // https://stackoverflow.com/a/19298151
    val x = scala.util.Random.nextInt(256)
    val randomColor = String.format("#%03X", x)
    Response.withBody(views.ColorThrobSnippet(randomColor))

  case GET -> Path("fade-out-on-swap") =>
    Response.withBody(views.FadeOutOnSwapView)
  case DELETE -> Path("fade_out_demo") =>
    Response.withBody("")

  case GET -> Path("fade-in-on-addition") =>
    Response.withBody(views.FadeInOnAdditionView)
  case POST -> Path("fade_in_demo") =>
    Response.withBody(views.theButton)

  case GET -> Path("request-in-flight") =>
    Response.withBody(views.RequestInFlightView)
  case POST -> Path("request-in-flight-name") =>
    Thread.sleep(1000) // simulate sloww
    Response.withBody("Submitted!")

UndertowSharafServer("localhost", 8181, routes).start()

println("Server started at http://localhost:8181")

object views {

  def IndexView = createPage(
    ul(
      li(a(href := "color-throb")("Color throb")),
      li(a(href := "fade-out-on-swap")("Fade Out On Swap")),
      li(a(href := "fade-in-on-addition")("Fade In On Addition")),
      li(a(href := "request-in-flight")("Request In Flight"))
    )
  )

  def ColorThrobView = createPage(
    ColorThrobSnippet("red"),
    inlineStyle = """
      .smooth {
        transition: all 1s ease-in;
      }
    """
  )

  def ColorThrobSnippet(color: String) = div(
    id := "color-demo", // must stay same!
    hx.get := "/colors",
    hx.swap := "outerHTML",
    hx.trigger := "every 1s",
    cls := "smooth",
    style := s"color:${color}"
  )("Color Swap Demo")

  def FadeOutOnSwapView = createPage(
    button(
      cls := "fade-me-out",
      hx.delete := "/fade_out_demo",
      hx.swap := "outerHTML swap:1s"
    )("Fade Me Out"),
    inlineStyle = """
      .fade-me-out.htmx-swapping {
        opacity: 0;
        transition: opacity 1s ease-out;
      }
    """
  )

  val theButton = button(
    id := "fade-me-in",
    hx.post := "/fade_in_demo",
    hx.swap := "outerHTML settle:1s"
  )("Fade Me In")

  def FadeInOnAdditionView = createPage(
    theButton,
    inlineStyle = """
      #fade-me-in.htmx-added {
        opacity: 0;
      }
      #fade-me-in {
        opacity: 1;
        transition: opacity 1s ease-out;
      }
    """
  )

  def RequestInFlightView = createPage(
    form(
      hx.post := "/request-in-flight-name",
      hx.swap := "outerHTML"
    )(
      label("Name: ", input(name := "name")),
      button("Submit")
    ),
    inlineStyle = """
      form.htmx-request {
        opacity: .5;
        transition: opacity 300ms linear;
      }
    """
  )

  private def createPage(bodyContent: Frag, inlineStyle: String = "") = doctype("html")(
    html(
      head(
        tag("style")(inlineStyle),
        script(src := "https://unpkg.com/htmx.org@2.0.4")
      ),
      body(bodyContent)
    )
  )
}
