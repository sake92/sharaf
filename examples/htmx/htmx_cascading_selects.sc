//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

// https://htmx.org/examples/value-select/

import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.querson.QueryStringRW
import ba.sake.sharaf.*
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
    div(
      div(
        label("Make"),
        select(
          name := "make",
          hx.get := "/models",
          hx.target := "#models",
          hx.swap := "outerHTML",
          hx.indicator := ".htmx-indicator"
        )(
          CarMake.values.map { make =>
            option(value := make.toString)(make.toString)
          }
        )
      ),
      div(
        label("Model"),
        cascadingSelect(make)
      ),
      img(src := "/img/bars.svg", alt := "Result loading...", cls := "htmx-indicator")
    )
  )

  def cascadingSelect(make: CarMake) = select(id := "models", name := "model")(
    make.models.map { model =>
      option(value := model)(model)
    }
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
