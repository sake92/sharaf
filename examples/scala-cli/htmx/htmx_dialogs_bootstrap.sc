//> using scala "3.4.2"
//> using dep ba.sake::sharaf:0.8.0

// https://htmx.org/examples/modal-bootstrap/

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.bootstrap5.BootstrapPage
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*, routing.*

object IndexView extends BootstrapPage with HtmxDependencies:
  override def pageContent = div(
    button(
      hx.get := "/modal",
      hx.trigger := "click",
      hx.target := "#modals-here",
      data.bs.toggle := "modal",
      data.bs.target := "#modals-here",
      cls := "btn btn-primary"
    )("Open Modal"),
    div(
      id := "modals-here",
      cls := "modal modal-blur fade",
      style := "display: none",
      aria.hidden := "false",
      tabindex := "-1"
    )(
      div(cls := "modal-dialog modal-lg modal-dialog-centered", role := "document")(
        div(cls := "modal-content")
      )
    )
  )

def bsDialog() = div(cls := "modal-dialog modal-dialog-centered")(
  div(cls := "modal-content")(
    div(cls := "modal-header")(
      h5(cls := "modal-title")("Modal title")
    ),
    div(cls := "modal-body")(p("Modal body text goes here.Modal body text goes here.")),
    div(cls := "modal-footer")(
      button(tpe := "button", cls := "btn btn-secondary", data.bs.dismiss := "modal")("Close")
    )
  )
)

val routes = Routes:
  case GET() -> Path() =>
    Response.withBody(IndexView)
  case GET() -> Path("modal") =>
    Response.withBody(bsDialog())

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
