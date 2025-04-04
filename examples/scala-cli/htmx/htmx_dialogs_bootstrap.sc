//> using scala "3.6.4"
//> using dep ba.sake::sharaf:0.9.2

// https://htmx.org/examples/modal-bootstrap/
// scala htmx_dialogs_bootstrap.sc --resource-dir resources

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*, routing.*

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(views.IndexView)
  case GET -> Path("modal") =>
    Response.withBody(views.bsDialog())

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")

object views {

  def IndexView = doctype("html")(
    html(
      head(
        link(rel := "stylesheet", href := "https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css"),
        script(src := "https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/js/bootstrap.bundle.min.js"),
        script(src := "https://unpkg.com/htmx.org@2.0.4")
      ),
      body(
        div(
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
      )
    )
  )

  def bsDialog() = div(cls := "modal-dialog modal-dialog-centered")(
    div(cls := "modal-content")(
      div(cls := "modal-header")(
        h5(cls := "modal-title")("Modal title")
      ),
      div(cls := "modal-body")(p("Modal body text goes here.")),
      div(cls := "modal-footer")(
        button(tpe := "button", cls := "btn btn-secondary", data.bs.dismiss := "modal")("Close")
      )
    )
  )
}
