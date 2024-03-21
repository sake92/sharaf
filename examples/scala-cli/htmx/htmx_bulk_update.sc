//> using scala "3.4.0"
//> using dep ba.sake::sharaf:0.4.0

// https://htmx.org/examples/bulk-update/
import io.undertow.Undertow
import ba.sake.sharaf.*, routing.*
import ba.sake.formson.FormDataRW

object views {
  import scalatags.Text.all.*
  import ba.sake.hepek.html.HtmlPage
  import ba.sake.hepek.htmx.*

  trait BasePage extends HtmlPage with HtmxDependencies

  class ContactsViewPage(contacts: Seq[Contact]) extends BasePage {
    override def bodyContent = div(
      h1("Bulk Updating example"),
      div(hx.include := "#checked-contacts", hx.target := "#tbody")(
        button(hx.put := "/activate")("Activate"),
        button(hx.put := "/deactivate")("Deactivate")
      ),
      form(id := "checked-contacts")(
        table(
          thead(tr(th(""), th("Name"), th("Email"), th("Status"))),
          tbody(id := "tbody")(
            contactsRows(contacts, AffectedContacts(Set.empty, false))
          )
        )
      )
    )

    override def stylesInline = List("""
     .htmx-settling tr.deactivate td {
        background: lightcoral;
      }
      .htmx-settling tr.activate td {
        background: darkseagreen;
      }
      tr td {
        transition: all 1.2s;
      }
    """)
  }

  def contactsRows(contacts: Seq[Contact], affectedContacts: AffectedContacts): Frag = contacts.map { contact =>
    val affectedClass = if affectedContacts.activated then "activate" else "deactivate"
    tr(
      Option.when(affectedContacts.ids(contact.id))(cls := affectedClass)
    )(
      td(input(name := "ids", value := contact.id, tpe := "checkbox")),
      td(contact.name),
      td(contact.email),
      td(if contact.active then "Active" else "Inactive")
    )
  }

}

case class Contact(id: Int, name: String, email: String, active: Boolean)

var currentContacts = Seq(
  Contact(1, "Joe Smith", "joe@smith.org", true),
  Contact(2, "Angie MacDowell", "angie@macdowell.org", true),
  Contact(3, "Fuqua Tarkenton", "fuqua@tarkenton.org", true),
  Contact(4, "Kim Yee", "kim@yee.org", false)
)

case class ContactIdsForm(ids: Set[Int]) derives FormDataRW

case class AffectedContacts(ids: Set[Int], activated: Boolean)

val routes = Routes:
  case GET() -> Path() =>
    Response.withBody(views.ContactsViewPage(currentContacts))
  case PUT() -> Path("activate") =>
    val formData = Request.current.bodyForm[ContactIdsForm]
    currentContacts = currentContacts.map { contact =>
      if formData.ids(contact.id) then contact.copy(active = true) else contact
    }
    Response.withBody(views.contactsRows(currentContacts, AffectedContacts(formData.ids, true)))
  case PUT() -> Path("deactivate") =>
    val formData = Request.current.bodyForm[ContactIdsForm]
    currentContacts = currentContacts.map { contact =>
      if formData.ids(contact.id) then contact.copy(active = false) else contact
    }
    Response.withBody(views.contactsRows(currentContacts, AffectedContacts(formData.ids, false)))

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
