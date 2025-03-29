//> using scala "3.6.4"
//> using dep ba.sake::sharaf:0.9.0

// https://htmx.org/examples/value-select/

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.html.HtmlPage
import ba.sake.hepek.htmx.*
import ba.sake.querson.QueryStringRW
import ba.sake.sharaf.*, routing.*

class IndexView(make: CarMake) extends HtmlPage with HtmxDependencies:
  override def pageContent = div(
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

def cascadingSelect(make: CarMake) = select(id := "models", name := "model")(
  make.models.map { model =>
    option(value := model)(model)
  }
)

enum CarMake(val models: Seq[String]) derives QueryStringRW:
  case Audi extends CarMake(Seq("A1", "A4", "A6"))
  case Toyota extends CarMake(Seq("Landcruiser", "Tacoma", "Yaris"))
  case BMW extends CarMake(Seq("325i", "325x", "X5"))

case class ModelsQP(make: CarMake) derives QueryStringRW

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView(CarMake.Audi))
  case GET -> Path("models") =>
    val qp = Request.current.queryParams[ModelsQP]
    Response.withBody(cascadingSelect(qp.make))

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
