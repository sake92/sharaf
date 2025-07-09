//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.13.0

// example of BS5 modal with a form
// https://htmx.org/examples/modal-bootstrap/

import ba.sake.formson.FormDataRW
import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(views.IndexView)
  case GET -> Path("modal") =>
    Response.withBody(views.bsDialog())
  case POST -> Path("submit-form") =>
    case class DialogForm(stuff: String) derives FormDataRW
    val formData = Request.current.bodyForm[DialogForm]
    Response.withBody(html""" <div> You submitted: $formData </div> """)

UndertowSharafServer("localhost", 8181, routes).start()

println("Server started at http://localhost:8181")

object views {
  def IndexView =
    html"""
    <!DOCTYPE html>
    <html>
    <head>
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css">
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/js/bootstrap.bundle.min.js"></script>
        <script src="https://unpkg.com/htmx.org@2.0.4"></script>
    </head>
    <body>
        <div class="container mt-5">
            <h1>HTMX Bootstrap Dialog Form Example</h1>
            <p>Click the button below to open a modal dialog with a form.</p>
            <button hx-get="/modal" hx-trigger="click" hx-target="#modals-here" data-bs-toggle="modal" data-bs-target="#modals-here" class="btn btn-primary">Open Modal</button>
            <div id="modals-here" class="modal modal-blur fade" tabindex="-1" aria-hidden="true">
                <div class="modal-dialog modal-lg modal-dialog-centered" role="document">
                    <div class="modal-content"></div>
                </div>
            </div>
            <div id="form-submission-result"></div>
        </div>
    </body>
    </html>
    """

  def bsDialog() =
    html"""
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Modal title</h5>
            </div>
            <div class="modal-body">
                <form hx-post="/submit-form" hx-target="#form-submission-result">
                    <label>Stuff: <input type="text" name="stuff"></label>
                    <button type="submit" class="btn btn-secondary" data-bs-dismiss="modal">Submit</button>
                </form>
            </div>
        </div>
    </div>
    """
}
