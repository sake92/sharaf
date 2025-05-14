//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

// https://htmx.org/examples/click-to-edit/

import scalatags.Text.all.{param =>_, *}
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*
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
    div(
      h1("Click to Edit example"),
      contactView(formData)
    )
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
