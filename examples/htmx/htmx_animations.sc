//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.13.0

// https://htmx.org/examples/animations/

import ba.sake.sharaf.{*, given}
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
    Thread.sleep(2000) // simulate sloww
    Response.withBody("Submitted!")

UndertowSharafServer("localhost", 8181, routes).start()

println("Server started at http://localhost:8181")

object views {

  def IndexView = createPage(
    html"""
    <ul>
        <li><a href="color-throb">Color throb</a></li>
        <li><a href="fade-out-on-swap">Fade Out On Swap</a></li>
        <li><a href="fade-in-on-addition">Fade In On Addition</a></li>
        <li><a href="request-in-flight">Request In Flight</a></li>
    </ul>
    """
  )

  def ColorThrobView = createPage(
    ColorThrobSnippet("red"),
    inlineStyle = """
      .smooth {
        transition: all 1s ease-in;
      }
    """
  )

  def ColorThrobSnippet(color: String) =
    // id must stay same!
    html"""
    <div id="color-demo"
        hx-get="/colors"
        hx-swap="outerHTML"
        hx-trigger="every 1s"
        class="smooth"
        style="color:${color}">
        Color Swap Demo
    </div>
    """

  def FadeOutOnSwapView = createPage(
    html"""
    <button class="fade-me-out"
      hx-delete="/fade_out_demo"
      hx-swap="outerHTML swap:1s">
      Fade Me Out
    """,
    inlineStyle = """
      .fade-me-out.htmx-swapping {
        opacity: 0;
        transition: opacity 1s ease-out;
      }
    """
  )

  val theButton = html"""
  <button id="fade-me-in"
    hx-post="/fade_in_demo"
    hx-swap="outerHTML settle:1s"
  >Fade Me In</button>
  """

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
    html"""
    <form
      hx-post="/request-in-flight-name"
      hx-swap="outerHTML"
      >
      <label>Name: <input name="name"></label>
      <button type="submit">Submit</button>
    </form>
    """,
    inlineStyle = """
      form.htmx-request {
        opacity: .5;
        transition: opacity 300ms linear;
      }
    """
  )

  private def createPage(bodyContent: Html, inlineStyle: String = "") =
    html"""
    <!DOCTYPE html>
    <html>
    <head>
        <script src="https://unpkg.com/htmx.org@2.0.4"></script>
        <style>
        ${inlineStyle}
        </style>
    </head>
    <body>
    ${bodyContent}
    </body>
    </html>
    """
}
