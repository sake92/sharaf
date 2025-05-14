//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

// https://htmx.org/examples/inline-validation/
// scala htmx_inline_validation.sc --resource-dir resources

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer
import ba.sake.formson.FormDataRW

val routes = Routes:
  case GET -> Path() =>
    val formData = ContactForm("", "", "")
    Response.withBody(views.IndexView(formData))
  case POST -> Path("contact", "email") =>
    val formData = Request.current.bodyForm[ContactForm]
    val isValid = formData.email == "test@test.com"
    Response.withBody(views.emailField(formData.email, isError = !isValid))
  case POST -> Path("contact") =>
    val formData = Request.current.bodyForm[ContactForm]
    Response.withBody(views.contactForm(formData))

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")

case class ContactForm(email: String, firstName: String, lastName: String) derives FormDataRW

object views {

  def IndexView(formData: ContactForm) = createPage(
    div(
      h3("Inline Validation example"),
      p("Only valid email is test@test.com"),
      contactForm(formData)
    ),
    inlineStyle = """
      .error-message {
        color:red;
      }
      .error input {
          box-shadow: 0 0 3px #CC0000;
      }
      .valid input {
          box-shadow: 0 0 3px #36cc00;
      }
    """
  )

  def contactForm(formData: ContactForm) = form(hx.post := "/contact", hx.swap := "outerHTML")(
    emailField(formData.email, isError = false),
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
      span("This will trigger validation on input change!"),
      Option.when(isError)(div(cls := "error-message")("That email is already taken.  Please enter another email."))
    )

  private def createPage(bodyContent: Frag, inlineStyle: String = "") = doctype("html")(
    html(
      head(
        tag("style")(inlineStyle),
        script(src := "https://unpkg.com/htmx.org@2.0.4")
      ),
      body(bodyContent)
    )
  )
}
