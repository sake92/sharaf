//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.12.1

// https://htmx.org/examples/value-select/

import play.twirl.api.Html
import ba.sake.querson.QueryStringRW
import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(views.IndexView(CarMake.Audi))
  case GET -> Path("models") =>
    case class ModelsQP(make: CarMake) derives QueryStringRW
    val qp = Request.current.queryParams[ModelsQP]
    Response.withBody(views.cascadingSelect(qp.make))

UndertowSharafServer("localhost", 8181, routes).start()

println("Server started at http://localhost:8181")

enum CarMake(val models: Seq[String]) derives QueryStringRW:
  case Audi extends CarMake(Seq("A1", "A4", "A6"))
  case Toyota extends CarMake(Seq("Landcruiser", "Tacoma", "Yaris"))
  case BMW extends CarMake(Seq("325i", "325x", "X5"))

object views {

  def IndexView(make: CarMake) = createPage(
    html"""
    <div>
        <div>
        <label for="make">Select a car make:</label>
        <select id="make" name="make"
                hx-get="/models"
                hx-target="#models"
                hx-swap="outerHTML"
                hx-indicator=".htmx-indicator">
          ${CarMake.values.map { make =>
        html"<option value='${make}'>${make}</option>"
      }}
        </select>
        </div>
        <div>
        <label for="models">Select a model:</label>
        ${cascadingSelect(make)}
        </div>
        <img src="/img/bars.svg" alt="Result loading..." class="htmx-indicator">
    </div>
    """
  )

  def cascadingSelect(make: CarMake) =
    html"""
    <select id="models" name="model">
      ${make.models.map { model =>
        html"<option value='${model}'>${model}</option>"
      }}
    </select>
    """

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
