//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.12.1

import play.twirl.api.{Html, HtmlFormat}
import ba.sake.formson.FormDataRW
import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer

// https://htmx.org/examples/file-upload/

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(views.IndexView)
  case POST -> Path("upload") =>
    case class FileUpload(file: java.nio.file.Path) derives FormDataRW
    val fileUpload = Request.current.bodyForm[FileUpload]
    Response.withBody(html"<div>Upload done!</div>")

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")

object views {

  def IndexView = createPage(
    html"""
    <form id="form" hx-post="/upload" hx-encoding="multipart/form-data">
        <input type="file" name="file" />
        <button type="submit">Upload</button>
        <progress id="progress" value="0" max="100"></progress>
    </form>
    """,
    inlineScript = """
      htmx.on('#form', 'htmx:xhr:progress', function(evt) {
        htmx.find('#progress').setAttribute('value', evt.detail.loaded/evt.detail.total * 100)
      });
    """
  )

  private def createPage(bodyContent: Html, inlineScript: String = "") =
    html"""
    <!DOCTYPE html>
    <html>
    <head>
        <script src="https://unpkg.com/htmx.org@2.0.4"></script>
        <script>
        ${HtmlFormat.raw(inlineScript)}
        </script>
    </head>
    <body>
    ${bodyContent}
    </body>
    </html>
    """

}
