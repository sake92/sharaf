//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.13.0

// https://htmx.org/examples/delete-row/

import ba.sake.sharaf.{*, given}
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

  def ContactsViewPage(contacts: Seq[Contact]) =
    html"""
    <!DOCTYPE html>
    <html>
    <head>
        <script src="https://unpkg.com/htmx.org@2.0.4"></script>
        <style>
        tr.htmx-swapping td {
            opacity: 0;
            transition: opacity 1s ease-out;
        }
        </style>
    </head>
    <body>
        <div>
            <h1>Delete Row example</h1>
            <table>
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Email</th>
                        <th></th>
                    </tr>
                </thead>
                <tbody hx-confirm="Are you sure?" hx-target="closest tr" hx-swap="outerHTML swap:1s">
                    ${contactsRows(contacts)}
                </tbody>
            </table>
        </div>
    </body>
    </html>
    """

  def contactsRows(contacts: Seq[Contact]) =
    val contactsHtml = contacts.map { contact =>
      html"""
        <tr>
            <td>${contact.name}</td>
            <td>${contact.email}</td>
            <td>
                <button hx-delete="/contacts/${contact.id}">Delete</button>
            </td>
        </tr>
        """
    }
    html"${contactsHtml}"

}
