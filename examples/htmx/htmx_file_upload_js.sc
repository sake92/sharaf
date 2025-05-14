//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.formson.FormDataRW
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

// https://htmx.org/examples/file-upload/
// scala htmx_file_upload_js.sc --resource-dir resources

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(views.IndexView)
  case POST -> Path("upload") =>
    case class FileUpload(file: java.nio.file.Path) derives FormDataRW
    val fileUpload = Request.current.bodyForm[FileUpload]
    Response.withBody(div(s"Upload done!"))

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")

object views {

  def IndexView = createPage(
    form(
      id := "form",
      hx.encoding := "multipart/form-data",
      hx.post := "/upload",
      input(`type` := "file", name := "file"),
      button("Upload"),
      tag("progress")(id := "progress", value := "0", max := "100")
    ),
    inlineScript = """
      htmx.on('#form', 'htmx:xhr:progress', function(evt) {
        htmx.find('#progress').setAttribute('value', evt.detail.loaded/evt.detail.total * 100)
      });
    """
  )

  private def createPage(bodyContent: Frag, inlineScript: String = "") = doctype("html")(
    html(
      head(
        script(src := "https://unpkg.com/htmx.org@2.0.4"),
        script(inlineScript)
      ),
      body(bodyContent)
    )
  )
}
