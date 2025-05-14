//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

// https://htmx.org/examples/delete-row/
// scala htmx_delete_row.sc --resource-dir resources

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

var allContacts = Seq(
  Contact("1", "Angie MacDowell", "angie@macdowell.org"),
  Contact("2", "Fuqua Tarkenton", "fuqua@tarkenton.org"),
  Contact("3", "Kim Yee", "kim@yee.org")
)

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(views.ContactsViewPage(allContacts))
  case DELETE -> Path("contacts", id) =>
    allContacts = allContacts.filterNot(_.id == id)
    Response.withBody("") // empty string, remove that <tr>

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")

case class Contact(id: String, name: String, email: String)

object views {

  def ContactsViewPage(contacts: Seq[Contact]) = doctype("html")(
    html(
      head(
        tag("style")("""
          tr.htmx-swapping td {
            opacity: 0;
            transition: opacity 1s ease-out;
          }
        """),
        script(src := "https://unpkg.com/htmx.org@2.0.4")
      ),
      body(
        div(
          h1("Delete Row example"),
          table()(
            thead(tr(th("Name"), th("Email"), th(""))),
            tbody(hx.confirm := "Are you sure?", hx.target := "closest tr", hx.swap := "outerHTML swap:1s")(
              contactsRows(contacts)
            )
          )
        )
      )
    )
  )

  def contactsRows(contacts: Seq[Contact]): Frag = contacts.map { contact =>
    tr(td(contact.name), td(contact.email), td(button(hx.delete := s"/contacts/${contact.id}")("Delete")))
  }

}
