//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

// example of BS5 modal with a form

import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.formson.FormDataRW
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(views.IndexView)
  case GET -> Path("modal") =>
    Response.withBody(views.bsDialog())
  case POST -> Path("submit-form") =>
    case class DialogForm(stuff: String) derives FormDataRW
    val formData = Request.current.bodyForm[DialogForm]
    Response.withBody(div(s"You submitted: $formData"))

UndertowSharafServer("localhost", 8181, routes).start()

println("Server started at http://localhost:8181")

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
          ),
          div(id := "form-submission-result")
        )
      )
    )
  )

  def bsDialog() = div(cls := "modal-dialog modal-dialog-centered")(
    div(cls := "modal-content")(
      div(cls := "modal-header")(
        h5(cls := "modal-title")("Modal title")
      ),
      div(cls := "modal-body")(
        form(hx.post := "/submit-form", hx.target := "#form-submission-result")(
          label("Stuff: ", input(tpe := "text", name := "stuff")),
          button(tpe := "submit", cls := "btn btn-secondary", data.bs.dismiss := "modal")("Submit")
        )
      )
    )
  )
}
