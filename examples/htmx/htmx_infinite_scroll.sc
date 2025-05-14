//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

// https://htmx.org/examples/click-to-load/

import java.util.UUID
import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.querson.QueryStringRW
import ba.sake.sharaf.*
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
    div(
      h1("Infinite Scroll example"),
      table(hx.indicator := ".htmx-indicator")(
        thead(tr(th("ID"), th("Name"), th("Email"))),
        tbody(
          contactsRows(contacts, page)
        )
      ),
      img(src := "/img/bars.svg", cls := "htmx-indicator")
    )
  )

  def contactsRows(contacts: Seq[Contact], page: Int): Frag =
    contacts.zipWithIndex.map { case (contact, idx) =>
      if idx == contacts.length - 1 then
        tr(hx.get := s"/contacts/?page=${page + 1}", hx.trigger := "revealed", hx.swap := "afterend")(
          td(contact.id),
          td(contact.name),
          td(contact.email)
        )
      else tr(td(contact.id), td(contact.name), td(contact.email))
    }

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
