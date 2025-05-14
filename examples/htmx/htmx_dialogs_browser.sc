//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

// https://htmx.org/examples/dialogs/

import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer
import ba.sake.sharaf.htmx.*

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(views.IndexView)
  case POST -> Path("submit") =>
    val submittedData = Request.current.headers(RequestHeaders.Prompt).head
    Response.withBody(
      div(
        p("You submitted data:"),
        submittedData
      )
    )

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")

object views {
  def IndexView = doctype("html")(
    html(
      head(
        script(src := "https://unpkg.com/htmx.org@2.0.4")
      ),
      body(
        div(
          button(
            hx.post := "/submit",
            hx.prompt := "Enter a string",
            hx.confirm := "Are you sure?",
            hx.target := "#response"
          )("Prompt Submission"),
          div(id := "response")
        )
      )
    )
  )

}
