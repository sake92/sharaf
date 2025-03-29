//> using scala "3.6.4"
//> using dep ba.sake::sharaf:0.9.0

// https://htmx.org/examples/click-to-load/
import java.util.UUID
import io.undertow.Undertow
import ba.sake.querson.QueryStringRW
import ba.sake.sharaf.*, routing.*

object views {
  import scalatags.Text.all.*
  import ba.sake.hepek.html.HtmlPage
  import ba.sake.hepek.htmx.*

  trait BasePage extends HtmlPage with HtmxDependencies

  class ContactsViewPage(contacts: Seq[Contact], page: Int) extends BasePage:
    override def pageContent = div(
      h1("Click to Load example"),
      table(
        thead(tr(th("ID"), th("Name"), th("Email"))),
        tbody(
          contactsRowsWithButton(contacts, page)
        )
      )
    )

  def contactsRowsWithButton(contacts: Seq[Contact], page: Int) = frag(
    contacts.map { contact =>
      tr(td(contact.id), td(contact.name), td(contact.email))
    },
    tr(id := "replaceMe")(
      td(colspan := "3")(
        button(
          hx.get := s"/contacts/?page=${page + 1}",
          hx.target := "#replaceMe",
          hx.swap := "outerHTML"
        )(
          "Load More Agents...",
          img(src := "/img/bars.svg", cls := "htmx-indicator")
        )
      )
    )
  )

}
case class Contact(id: String, name: String, email: String)
object Contact:
  def create(): Contact =
    val id = UUID.randomUUID().toString
    Contact(id, "Agent Smith", s"agent_smith_${id.take(8)}@example.com")

case class PageQP(page: Int) derives QueryStringRW

val PageSize = 5

val allContacts = Seq.fill(100)(Contact.create())

val routes = Routes:
  case GET -> Path() =>
    val contactsSlice = allContacts.take(PageSize)
    Response.withBody(views.ContactsViewPage(contactsSlice, 0))
  case GET -> Path("contacts") =>
    Thread.sleep(500) // simulate slow backend :)
    val qp = Request.current.queryParams[PageQP]
    val contactsSlice = allContacts.drop(qp.page * PageSize).take(PageSize)
    Response.withBody(views.contactsRowsWithButton(contactsSlice, qp.page))

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
