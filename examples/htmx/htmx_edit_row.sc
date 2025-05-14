//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

// https://htmx.org/examples/edit-row/
// scala htmx_edit_row.sc --resource-dir resources

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.formson.FormDataRW
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

var allContacts = Seq(
  Contact("1", "Joe Smith", "joe@smith.org"),
  Contact("2", "Angie MacDowell", "angie@macdowell.org"),
  Contact("3", "Fuqua Tarkenton", "fuqua@tarkenton.org"),
  Contact("4", "Kim Yee", "kim@yee.org")
)

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(views.ContactsViewPage(allContacts))
  case GET -> Path("contact", id) =>
    val contactOpt = allContacts.find(_.id == id)
    val rowOpt = contactOpt.map(views.viewContactRow)
    Response.withBodyOpt(rowOpt, "contact")
  case GET -> Path("contact", id, "edit") =>
    val contactOpt = allContacts.find(_.id == id)
    val rowOpt = contactOpt.map(views.editContact)
    Response.withBodyOpt(rowOpt, "contact")
  case PUT -> Path("contact", id) =>
    val formData = Request.current.bodyForm[ContactForm]
    val idx = allContacts.indexWhere(_.id == id)
    val updatedContact = allContacts(idx).copy(
      name = formData.name,
      email = formData.email
    )
    allContacts = allContacts.updated(idx, updatedContact)
    Response.withBody(views.viewContactRow(updatedContact))

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")

case class Contact(id: String, name: String, email: String)

case class ContactForm(name: String, email: String) derives FormDataRW

object views {

  def ContactsViewPage(contacts: Seq[Contact]) = createPage(
    div(
      h1("Click to Edit example"),
      table(
        thead(tr(th("Name"), th("Email"), th())),
        tbody(hx.target := "closest tr", hx.swap := "outerHTML")(
          contacts.map(viewContactRow)
        )
      )
    )
  )

  def viewContactRow(contact: Contact) = tr(
    td(contact.name),
    td(contact.email),
    td(
      button(
        hx.get := s"/contact/${contact.id}/edit",
        hx.trigger := "edit",
        onclick := """
              let editing = document.querySelector('.editing')
              if (editing) {
                const doWant = confirm("You are already editing a row!  Do you want to cancel that edit and continue?");
                if (doWant) {
                  htmx.trigger(editing, 'cancel')
                  htmx.trigger(this, 'edit')
                }
              } else {
                htmx.trigger(this, 'edit')
              }"""
      )("Edit")
    )
  )

  def editContact(contact: Contact) = tr(
    hx.trigger := "cancel",
    hx.get := s"/contact/${contact.id}"
  )(
    td(input(name := "name", value := contact.name, autofocus)),
    td(input(name := "email", value := contact.email)),
    td(
      button(hx.get := s"/contact/${contact.id}")("Cancel"),
      button(hx.put := s"/contact/${contact.id}", hx.include := "closest tr")("Save")
    )
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
