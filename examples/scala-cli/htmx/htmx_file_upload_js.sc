//> using scala "3.6.4"
//> using dep ba.sake::sharaf:0.9.2

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.formson.FormDataRW
import ba.sake.sharaf.*, routing.*

// https://htmx.org/examples/file-upload/
// scala htmx_file_upload_js.sc --resource-dir resources

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(views.IndexView)
  case POST -> Path("upload") =>
    case class FileUpload(file: java.nio.file.Path) derives FormDataRW
    val fileUpload = Request.current.bodyForm[FileUpload]
    Response.withBody(div(s"Upload done!"))

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

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
