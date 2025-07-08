//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.12.1

// https://htmx.org/examples/click-to-edit/

import play.twirl.api.Html
import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer
import ba.sake.formson.FormDataRW

var currentValue = ContactForm("Joe", "Blow", "joe@blow.com")

val routes = Routes:
  case GET -> Path() =>
    Response.redirect("/contact/1")
  case GET -> Path("contact", param[Int](id)) =>
    Response.withBody(views.ContactViewPage(currentValue))
  case GET -> Path("contact", param[Int](id), "edit") =>
    Response.withBody(views.contactEdit(currentValue))
  case PUT -> Path("contact", param[Int](id)) =>
    val formData = Request.current.bodyForm[ContactForm]
    currentValue = formData
    Response.withBody(views.contactView(currentValue))

UndertowSharafServer("localhost", 8181, routes).start()

println("Server started at http://localhost:8181")

case class ContactForm(firstName: String, lastName: String, email: String) derives FormDataRW

object views {

  def ContactViewPage(formData: ContactForm) = createPage(
    html"""
    <div>
        <h1>Click to Edit example</h1>
        ${contactView(formData)}
    </div>
    """
  )

  def contactView(formData: ContactForm) =
    html"""
    <div hx-target="this" hx-swap="outerHTML">
        <h2>Contact Details</h2>
        <div><label>First Name</label>: ${formData.firstName}</div>
        <div><label>Last Name</label>: ${formData.lastName}</div>
        <div><label>Email</label>: ${formData.email}</div>
        <button hx-get="/contact/1/edit">Click To Edit</button>
    </div>
    """

  def contactEdit(formData: ContactForm) =
    html"""
    <form hx-put="/contact/1" hx-target="this" hx-swap="outerHTML">
        <div>
            <label>First Name</label>
            <input type="text" name="firstName" value="${formData.firstName}">
        </div>
        <div>
            <label>Last Name</label>
            <input type="text" name="lastName" value="${formData.lastName}">
        </div>
        <div>
            <label>Email</label>
            <input type="email" name="email" value="${formData.email}">
        </div>
        <button type="submit">Submit</button>
        <button hx-get="/contact/1">Cancel</button>
    </form>
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
