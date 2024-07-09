//> using scala "3.4.2"
//> using dep ba.sake::sharaf:0.7.0

// https://htmx.org/examples/click-to-edit/
import io.undertow.Undertow
import ba.sake.sharaf.*, routing.*
import ba.sake.formson.FormDataRW

object views {
  import scalatags.Text.all.*
  import ba.sake.hepek.html.HtmlPage
  import ba.sake.hepek.htmx.*

  trait BasePage extends HtmlPage with HtmxDependencies

  class ContactViewPage(formData: ContactForm) extends BasePage:
    override def bodyContent = div(
      h1("Click to Edit example"),
      contactView(formData)
    )

  def contactView(formData: ContactForm) = div(hx.target := "this", hx.swap := "outerHTML")(
    div(label("First Name"), s": ${formData.firstName}"),
    div(label("Last Name"), s": ${formData.lastName}"),
    div(label("Email"), s": ${formData.email}"),
    button(hx.get := "/contact/1/edit")("Click To Edit")
  )

  def contactEdit(formData: ContactForm) = form(hx.put := "/contact/1", hx.target := "this", hx.swap := "outerHTML")(
    div(label("First Name"), input(tpe := "text", name := "firstName", value := formData.firstName)),
    div(label("Last Name"), input(tpe := "text", name := "lastName", value := formData.lastName)),
    div(label("Email"), input(tpe := "email", name := "email", value := formData.email)),
    button("Submit"),
    button(hx.get := "/contact/1")("Cancel")
  )
}

case class ContactForm(firstName: String, lastName: String, email: String) derives FormDataRW

var currentValue = ContactForm("Joe", "Blow", "joe@blow.com")

val routes = Routes:
  case GET() -> Path() =>
    Response.redirect("/contact/1")
  case GET() -> Path("contact", param[Int](id)) =>
    Response.withBody(views.ContactViewPage(currentValue))
  case GET() -> Path("contact", param[Int](id), "edit") =>
    Response.withBody(views.contactEdit(currentValue))
  case PUT() -> Path("contact", param[Int](id)) =>
    val formData = Request.current.bodyForm[ContactForm]
    currentValue = formData
    Response.withBody(views.contactView(currentValue))

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
