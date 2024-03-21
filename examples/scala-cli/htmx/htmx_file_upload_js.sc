//> using scala "3.4.0"
//> using dep ba.sake::sharaf:0.4.0

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.html.HtmlPage
import ba.sake.hepek.htmx.*
import ba.sake.formson.FormDataRW
import ba.sake.sharaf.*, routing.*

// https://htmx.org/examples/file-upload/

object IndexView extends HtmlPage with HtmxDependencies:
  override def bodyContent = form(
    id := "form",
    hx.encoding := "multipart/form-data",
    hx.post := "/upload",
    input(`type` := "file", name := "file"),
    button("Upload"),
    tag("progress")(id := "progress", value := "0", max := "100")
  )
  override def scriptsInline = List("""
    htmx.on('#form', 'htmx:xhr:progress', function(evt) {
      htmx.find('#progress').setAttribute('value', evt.detail.loaded/evt.detail.total * 100)
    });
  """)

case class FileUpload(file: java.nio.file.Path) derives FormDataRW

val routes = Routes:
  case GET() -> Path() =>
    Response.withBody(IndexView)
  case POST() -> Path("upload") =>
    val fileUpload = Request.current.bodyForm[FileUpload]
    Response.withBody(div("Upload done!"))

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
