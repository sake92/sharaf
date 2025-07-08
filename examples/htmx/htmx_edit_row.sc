//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.12.1

// https://htmx.org/examples/edit-row/

import play.twirl.api.Html
import ba.sake.formson.FormDataRW
import ba.sake.sharaf.{*, given}
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
    html"""
    <div>
        <h1>Click to Edit example</h1>
        <table>
            <thead>
                <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th></th>
                </tr>
            </thead>
            <tbody hx-target="closest tr" hx-swap="outerHTML">
                ${contacts.map(viewContactRow)}
            </tbody>
        </table>
    </div>
    """
  )

  def viewContactRow(contact: Contact) =
    html"""
    <tr>
        <td>${contact.name}</td>
        <td>${contact.email}</td>
        <td>
            <button hx-get="/contact/${contact.id}/edit" hx-trigger="edit" 
                onclick="
                let editing = document.querySelector('.editing')
                if (editing) {
                    const doWant = confirm('You are already editing a row!  Do you want to cancel that edit and continue?');
                    if (doWant) {
                        htmx.trigger(editing, 'cancel');
                        htmx.trigger(this, 'edit');
                    }
                } else {
                    htmx.trigger(this, 'edit')
                }"
            >
            Edit
            </button>
        </td>
    </tr>
    """

  def editContact(contact: Contact) =
    html"""
    <tr hx-trigger="cancel" hx-get="/contact/${contact.id}">
        <td><input name="name" value="${contact.name}" autofocus></td>
        <td><input name="email" value="${contact.email}"></td>
        <td>
            <button hx-get="/contact/${contact.id}">Cancel</button>
            <button hx-put="/contact/${contact.id}" hx-include="closest tr">Save</button>
        </td>
    </tr>
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
