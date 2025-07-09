//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.13.0

// https://htmx.org/examples/bulk-update/


import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer
import ba.sake.formson.FormDataRW

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(views.ContactsViewPage(currentContacts))
  case PUT -> Path("activate") =>
    val formData = Request.current.bodyForm[ContactIdsForm]
    currentContacts = currentContacts.map { contact =>
      if formData.ids(contact.id) then contact.copy(active = true) else contact
    }
    Response.withBody(views.contactsRows(currentContacts, AffectedContacts(formData.ids, true)))
  case PUT -> Path("deactivate") =>
    val formData = Request.current.bodyForm[ContactIdsForm]
    currentContacts = currentContacts.map { contact =>
      if formData.ids(contact.id) then contact.copy(active = false) else contact
    }
    Response.withBody(views.contactsRows(currentContacts, AffectedContacts(formData.ids, false)))

UndertowSharafServer("localhost", 8181, routes).start()

println("Server started at http://localhost:8181")

object views {

  def ContactsViewPage(contacts: Seq[Contact]) = createPage(
    html"""
    <div>
        <h1>Bulk Updating example</h1>
        <div hx-include="#checked-contacts" hx-target="#tbody">
            <button hx-put="/activate">Activate</button>
            <button hx-put="/deactivate">Deactivate</button>
        </div>
        <form id="checked-contacts">
            <table>
                <thead>
                    <tr>
                        <th></th>
                        <th>Name</th>
                        <th>Email</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody id="tbody">
                    ${contactsRows(contacts, AffectedContacts(Set.empty, false))}
                </tbody>
            </table>
        </form>
    </div>
    """,
    inlineStyle = """
     .htmx-settling tr.deactivate td {
        background: lightcoral;
      }
      .htmx-settling tr.activate td {
        background: darkseagreen;
      }
      tr td {
        transition: all 1.2s;
      }
    """
  )

  def contactsRows(contacts: Seq[Contact], affectedContacts: AffectedContacts): Html =
    val contactsHtml = contacts
      .map { contact =>
        val affectedClass = if affectedContacts.activated then "activate" else "deactivate"
        html"""
        <tr ${Option.when(affectedContacts.ids(contact.id))(html"class=${affectedClass}")}>
            <td><input name="ids" value="${contact.id}" type="checkbox"></td>
            <td>${contact.name}</td>
            <td>${contact.email}</td>
            <td>${if contact.active then "Active" else "Inactive"}</td>
        </tr>
        """
      }
    html"${contactsHtml}"

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

case class Contact(id: Int, name: String, email: String, active: Boolean)

var currentContacts = Seq(
  Contact(1, "Joe Smith", "joe@smith.org", true),
  Contact(2, "Angie MacDowell", "angie@macdowell.org", true),
  Contact(3, "Fuqua Tarkenton", "fuqua@tarkenton.org", true),
  Contact(4, "Kim Yee", "kim@yee.org", false)
)

case class ContactIdsForm(ids: Set[Int]) derives FormDataRW

case class AffectedContacts(ids: Set[Int], activated: Boolean)
