//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.13.0

// https://htmx.org/examples/inline-validation/

import ba.sake.sharaf.{*, given}
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
    html"""
    <div>
        <h3>HTMX Inline Validation Example</h3>
        <p>Only valid email is test@test.com</p>
        ${contactForm(formData)}
    </div>
    """,
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

  def contactForm(formData: ContactForm) =
    html"""
    <form hx-post="/contact" hx-swap="outerHTML">
        ${emailField(formData.email, isError = false)}
        <div>
            <label>First Name</label>
            <input name="firstName" value="${formData.firstName}">
        </div>
        <div>
            <label>Last Name</label>
            <input name="lastName" value="${formData.lastName}">
        </div>
        <button type="submit">Submit</button>
    </form>
    """

  def emailField(fieldValue: String, isError: Boolean) =
    val cls = if (isError) "error" else ""
    html"""
    <div hx-target="this" hx-swap="outerHTML" class="${cls}">
        <label>Email Address
            <input name="email" value="${fieldValue}" hx-post="/contact/email" hx-indicator="#ind">
            <img id="ind" src="/img/bars.svg" class="htmx-indicator">
        </label>
        <span>This will trigger validation on input change!</span>
        ${Option.when(isError)(
        html"""<div class="error-message">That email is already taken.  Please enter another email.</div>"""
      )}
    </div>
    """

  private def createPage(bodyContent: Html, inlineStyle: String = "") =
    html"""
    <!DOCTYPE html>
    <html>
    <head>
        <script src="https://unpkg.com/htmx.org@2.0.4"></script>
        <style>
        ${inlineStyle}
        </style>
    </head>
    <body>
    ${bodyContent}
    </body>
    </html>
    """
}
