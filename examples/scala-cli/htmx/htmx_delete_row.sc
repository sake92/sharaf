//> using scala "3.4.2"
//> using dep ba.sake::sharaf:0.7.0

// https://htmx.org/examples/delete-row/

import io.undertow.Undertow
import ba.sake.sharaf.*, routing.*

object views {
  import scalatags.Text.all.*
  import ba.sake.hepek.html.HtmlPage
  import ba.sake.hepek.htmx.*

  trait BasePage extends HtmlPage with HtmxDependencies

  class ContactsViewPage(contacts: Seq[Contact]) extends BasePage:
    override def pageContent = div(
      h1("Delete Row example"),
      table()(
        thead(tr(th("Name"), th("Email"), th(""))),
        tbody(hx.confirm := "Are you sure?", hx.target := "closest tr", hx.swap := "outerHTML swap:1s")(
          contactsRows(contacts)
        )
      )
    )

    override def stylesInline = List("""
      tr.htmx-swapping td {
        opacity: 0;
        transition: opacity 1s ease-out;
      }
    """)

  def contactsRows(contacts: Seq[Contact]): Frag = contacts.map { contact =>
    tr(td(contact.name), td(contact.email), td(button(hx.delete := s"/contacts/${contact.id}")("Delete")))
  }

}
case class Contact(id: String, name: String, email: String)

var allContacts = Seq(
  Contact("1", "Angie MacDowell", "angie@macdowell.org"),
  Contact("2", "Fuqua Tarkenton", "fuqua@tarkenton.org"),
  Contact("3", "Kim Yee", "kim@yee.org")
)

val routes = Routes:
  case GET() -> Path() =>
    Response.withBody(views.ContactsViewPage(allContacts))
  case DELETE() -> Path("contacts", id) =>
    allContacts = allContacts.filterNot(_.id == id)
    Response.withBody("") // empty string, remove that <tr>

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
