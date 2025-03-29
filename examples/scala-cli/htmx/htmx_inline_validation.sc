//> using scala "3.6.4"
//> using dep ba.sake::sharaf:0.9.0

// https://htmx.org/examples/inline-validation/

import io.undertow.Undertow
import ba.sake.sharaf.*, routing.*
import ba.sake.formson.FormDataRW

object views {
  import scalatags.Text.all.*
  import ba.sake.hepek.html.HtmlPage
  import ba.sake.hepek.htmx.*

  class IndexView(formData: ContactForm) extends HtmlPage with HtmxDependencies:
    override def pageContent = div(
      h3("Inline Validation example"),
      p("Only valid email is test@test.com"),
      contactForm(formData)
    )

    override def stylesInline = List("""
      .error-message {
        color:red;
      }
      .error input {
          box-shadow: 0 0 3px #CC0000;
      }
      .valid input {
          box-shadow: 0 0 3px #36cc00;
      }
    """)

  def contactForm(formData: ContactForm) = form(hx.post := "/contact", hx.swap := "outerHTML")(
    emailField(formData.email, false),
    div(label("First Name")(input(name := "firstName", value := formData.firstName))),
    div(label("Last Name")(input(name := "lastName", value := formData.lastName))),
    button("Submit")
  )

  def emailField(fieldValue: String, isError: Boolean) =
    div(hx.target := "this", hx.swap := "outerHTML", Option.when(isError)(cls := "error"))(
      label("Email Address")(
        input(name := "email", value := fieldValue, hx.post := "/contact/email", hx.indicator := "#ind"),
        img(id := "ind", src := "/img/bars.svg", cls := "htmx-indicator")
      ),
      Option.when(isError)(div(cls := "error-message")("That email is already taken.  Please enter another email."))
    )

}

case class ContactForm(email: String, firstName: String, lastName: String) derives FormDataRW

val routes = Routes:
  case GET -> Path() =>
    val formData = ContactForm("", "", "")
    Response.withBody(views.IndexView(formData))
  case POST -> Path("contact", "email") =>
    val formData = Request.current.bodyForm[ContactForm]
    val isValid = formData.email == "test@test.com"
    Response.withBody(views.emailField(formData.email, !isValid))
  case POST -> Path("contact") =>
    val formData = Request.current.bodyForm[ContactForm]
    Response.withBody(views.contactForm(formData))

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
