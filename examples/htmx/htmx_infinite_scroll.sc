//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.12.1

// https://htmx.org/examples/click-to-load/

import java.util.UUID
import play.twirl.api.Html
import ba.sake.querson.QueryStringRW
import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer

val PageSize = 10

val allContacts = Seq.fill(100)(Contact.create())

val routes = Routes:
  case GET -> Path() =>
    val contactsSlice = allContacts.take(PageSize)
    Response.withBody(views.ContactsViewPage(contactsSlice, 0))
  case GET -> Path("contacts") =>
    Thread.sleep(500) // simulate slow backend :)
    case class PageQP(page: Int) derives QueryStringRW
    val qp = Request.current.queryParams[PageQP]
    val contactsSlice = allContacts.slice(qp.page * PageSize, qp.page * PageSize + PageSize)
    Response.withBody(views.contactsRows(contactsSlice, qp.page))

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")

case class Contact(id: String, name: String, email: String)
object Contact:
  def create(): Contact =
    val id = UUID.randomUUID().toString
    Contact(id, "Agent Smith", s"agent_smith_${id.take(8)}@example.com")

object views {

  def ContactsViewPage(contacts: Seq[Contact], page: Int) = createPage(
    html"""
    <div>
        <h1>Infinite Scroll example</h1>
        <table hx-indicator=".htmx-indicator">
            <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
            </tr>
            </thead>
            <tbody>
            ${contactsRows(contacts, page)}
            </tbody>
        </table>
        <img src="/img/bars.svg" class="htmx-indicator" alt="Loading indicator">
    </div>
    """
  )

  def contactsRows(contacts: Seq[Contact], page: Int): Html =
    val contactsHtmls = contacts.zipWithIndex.map { case (contact, idx) =>
      if idx == contacts.length - 1 then
        html"""
        <tr hx-get="/contacts/?page=${page + 1}" hx-trigger="revealed" hx-swap="afterend">
            <td>${contact.id}</td>
            <td>${contact.name}</td>
            <td>${contact.email}</td>
        </tr>
        """
      else 
        html"""
        <tr>
            <td>${contact.id}</td>
            <td>${contact.name}</td>
            <td>${contact.email}</td>
        </tr>
        """
    }
    html"${contactsHtmls}"

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
