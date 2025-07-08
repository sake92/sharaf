//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.12.1

// https://htmx.org/examples/active-search/

import ba.sake.formson.FormDataRW
import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(views.ContactsViewPage(Seq.empty))
  case POST -> Path("search") =>
    Thread.sleep(500) // simulate slow backend :)
    case class SearchForm(search: String) derives FormDataRW
    val formData = Request.current.bodyForm[SearchForm]
    val contactsSlice = allContacts.filter(_.matches(formData.search))
    Response.withBody(views.contactsRows(contactsSlice))

UndertowSharafServer("localhost", 8181, routes).start()

println("Server started at http://localhost:8181")

object views {

  def ContactsViewPage(contacts: Seq[Contact]) =
    html"""
    <!DOCTYPE html>
    <html>
    <head>
      <script src="https://unpkg.com/htmx.org@2.0.4"></script>
    </head>
    <body>
      <div class="htmx-indicator" >
        <img src="/img/bars.svg" alt="Loading...">
        Searching... 
      </div>
      <h1>Active Search example</h1>
      <input
        type="search"
        name="search"
        placeholder="Begin Typing To Search Users..."
        hx-post="/search"
        hx-trigger="input changed delay:500ms, search"
        hx-target="#search-results"
        hx-indicator=".htmx-indicator"
      >
      <table>
        <thead>
          <tr>
            <th>First Name</th>
            <th>Last Name</th>
            <th>Email</th>
          </tr>
        </thead>
        <tbody id="search-results">
          ${contactsRows(contacts)}
        </tbody>
      </table>
    </body>
    </html>
  """

  def contactsRows(contacts: Seq[Contact]) =
    val contactsHtml = contacts.zipWithIndex
      .map { case (contact, idx) =>
        html"""
        <tr>
          <td>${contact.firstName}</td>
          <td>${contact.lastName}</td>
          <td>${contact.email}</td>
        </tr>
      """
      }
    html"${contactsHtml}"

}

case class Contact(firstName: String, lastName: String, email: String):
  def matches(str: String): Boolean =
    val lowerStr = str.trim.toLowerCase
    firstName.toLowerCase.contains(lowerStr) ||
    lastName.toLowerCase.contains(lowerStr) ||
    email.toLowerCase.contains(lowerStr)

val allContacts = Seq(
  Contact("Venus", "Grimes", "lectus.rutrum@Duisa.edu"),
  Contact("Fletcher", "Owen", "metus@Aenean.org"),
  Contact("William", "Hale", "eu.dolor@risusodio.edu"),
  Contact("TaShya", "Cash", "tincidunt.orci.quis@nuncnullavulputate.co.uk"),
  Contact("Kevyn", "Hoover", "tristique.pellentesque.tellus@Cumsociis.co.uk"),
  Contact("Jakeem", "Walker", "Morbi.vehicula.Pellentesque@faucibusorci.org"),
  Contact("Malcolm", "Trujillo", "sagittis@velit.edu"),
  Contact("Wynne", "Rice", "augue.id@felisorciadipiscing.edu"),
  Contact("Evangeline", "Klein", "adipiscing.lobortis@sem.org"),
  Contact("Jennifer", "Russell", "sapien.Aenean.massa@risus.com"),
  Contact("Rama", "Freeman", "Proin@quamPellentesquehabitant.net"),
  Contact("Jena", "Mathis", "non.cursus.non@Phaselluselit.com"),
  Contact("Alexandra", "Maynard", "porta.elit.a@anequeNullam.ca"),
  Contact("Tallulah", "Haley", "ligula@id.net"),
  Contact("Timon", "Small", "velit.Quisque.varius@gravidaPraesent.org"),
  Contact("Randall", "Pena", "facilisis@Donecconsectetuer.edu"),
  Contact("Conan", "Vaughan", "luctus.sit@Classaptenttaciti.edu"),
  Contact("Dora", "Allen", "est.arcu.ac@Vestibulumante.co.uk"),
  Contact("Aiko", "Little", "quam.dignissim@convallisest.net"),
  Contact("Jessamine", "Bauer", "taciti.sociosqu@nibhvulputatemauris.co.uk"),
  Contact("Gillian", "Livingston", "justo@atiaculisquis.com"),
  Contact("Laith", "Nicholson", "elit.pellentesque.a@diam.org"),
  Contact("Paloma", "Alston", "cursus@metus.org"),
  Contact("Freya", "Dunn", "Vestibulum.accumsan@metus.co.uk"),
  Contact("Griffin", "Rice", "justo@tortordictumeu.net"),
  Contact("Catherine", "West", "malesuada.augue@elementum.com"),
  Contact("Jena", "Chambers", "erat.Etiam.vestibulum@quamelementumat.net"),
  Contact("Neil", "Rodriguez", "enim@facilisis.com"),
  Contact("Freya", "Charles", "metus@nec.net"),
  Contact("Anastasia", "Strong", "sit@vitae.edu"),
  Contact("Bell", "Simon", "mollis.nec.cursus@disparturientmontes.ca"),
  Contact("Minerva", "Allison", "Donec@nequeIn.edu"),
  Contact("Yoko", "Dawson", "neque.sed@semper.net"),
  Contact("Nadine", "Justice", "netus@et.edu"),
  Contact("Hoyt", "Rosa", "Nullam.ut.nisi@Aliquam.co.uk"),
  Contact("Shafira", "Noel", "tincidunt.nunc@non.edu"),
  Contact("Jin", "Nunez", "porttitor.tellus.non@venenatisamagna.net"),
  Contact("Barbara", "Gay", "est.congue.a@elit.com"),
  Contact("Riley", "Hammond", "tempor.diam@sodalesnisi.net"),
  Contact("Molly", "Fulton", "semper@Naminterdumenim.net"),
  Contact("Dexter", "Owen", "non.ante@odiosagittissemper.ca"),
  Contact("Kuame", "Merritt", "ornare.placerat.orci@nisinibh.ca"),
  Contact("Maggie", "Delgado", "Nam.ligula.elit@Cum.org"),
  Contact("Hanae", "Washington", "nec.euismod@adipiscingelit.org"),
  Contact("Jonah", "Cherry", "ridiculus.mus.Proin@quispede.edu"),
  Contact("Cheyenne", "Munoz", "at@molestiesodalesMauris.edu"),
  Contact("India", "Mack", "sem.mollis@Inmi.co.uk"),
  Contact("Lael", "Mcneil", "porttitor@risusDonecegestas.com"),
  Contact("Jillian", "Mckay", "vulputate.eu.odio@amagnaLorem.co.uk"),
  Contact("Shaine", "Wright", "malesuada@pharetraQuisqueac.org"),
  Contact("Keane", "Richmond", "nostra.per.inceptos@euismodurna.org"),
  Contact("Samuel", "Davis", "felis@euenim.com"),
  Contact("Zelenia", "Sheppard", "Quisque.nonummy@antelectusconvallis.org"),
  Contact("Giacomo", "Cole", "aliquet.libero@urnaUttincidunt.ca"),
  Contact("Mason", "Hinton", "est@Nunc.co.uk"),
  Contact("Katelyn", "Koch", "velit.Aliquam@Suspendisse.edu"),
  Contact("Olga", "Spencer", "faucibus@Praesenteudui.net"),
  Contact("Erasmus", "Strong", "dignissim.lacus@euarcu.net"),
  Contact("Regan", "Cline", "vitae.erat.vel@lacusEtiambibendum.co.uk"),
  Contact("Stone", "Holt", "eget.mollis.lectus@Aeneanegestas.ca"),
  Contact("Deanna", "Branch", "turpis@estMauris.net"),
  Contact("Rana", "Green", "metus@conguea.edu"),
  Contact("Caryn", "Henson", "Donec.sollicitudin.adipiscing@sed.net"),
  Contact("Clarke", "Stein", "nec@mollis.co.uk"),
  Contact("Kelsie", "Porter", "Cum@gravidaAliquam.com"),
  Contact("Cooper", "Pugh", "Quisque.ornare.tortor@dictum.co.uk"),
  Contact("Paul", "Spencer", "ac@InfaucibusMorbi.com"),
  Contact("Cassady", "Farrell", "Suspendisse.non@venenatisa.net"),
  Contact("Sydnee", "Velazquez", "mollis@loremfringillaornare.com"),
  Contact("Felix", "Boyle", "id.libero.Donec@aauctor.org"),
  Contact("Ryder", "House", "molestie@natoquepenatibus.org"),
  Contact("Hadley", "Holcomb", "penatibus@nisi.ca"),
  Contact("Marsden", "Nunez", "Nulla.eget.metus@facilisisvitaeorci.org"),
  Contact("Alana", "Powell", "non.lobortis.quis@interdumfeugiatSed.net"),
  Contact("Dennis", "Wyatt", "Morbi.non@nibhQuisquenonummy.ca"),
  Contact("Karleigh", "Walton", "nascetur.ridiculus@quamdignissimpharetra.com"),
  Contact("Brielle", "Donovan", "placerat@at.edu"),
  Contact("Donna", "Dickerson", "lacus.pede.sagittis@lacusvestibulum.com"),
  Contact("Eagan", "Pate", "est.Nunc@cursusNunc.ca"),
  Contact("Carlos", "Ramsey", "est.ac.facilisis@duinec.co.uk"),
  Contact("Regan", "Murphy", "lectus.Cum@aptent.com"),
  Contact("Claudia", "Spence", "Nunc.lectus.pede@aceleifend.co.uk"),
  Contact("Genevieve", "Parker", "ultrices@inaliquetlobortis.net"),
  Contact("Marshall", "Allison", "erat.semper.rutrum@odio.org"),
  Contact("Reuben", "Davis", "Donec@auctorodio.edu"),
  Contact("Ralph", "Doyle", "pede.Suspendisse.dui@Curabitur.org"),
  Contact("Constance", "Gilliam", "mollis@Nulla.edu"),
  Contact("Serina", "Jacobson", "dictum.augue@ipsum.net"),
  Contact("Charity", "Byrd", "convallis.ante.lectus@scelerisquemollisPhasellus.co.uk"),
  Contact("Hyatt", "Bird", "enim.Nunc.ut@nonmagnaNam.com"),
  Contact("Brent", "Dunn", "ac.sem@nuncid.com"),
  Contact("Casey", "Bonner", "id@ornareelitelit.edu"),
  Contact("Hakeem", "Gill", "dis@nonummyipsumnon.org"),
  Contact("Stewart", "Meadows", "Nunc.pulvinar.arcu@convallisdolorQuisque.net"),
  Contact("Nomlanga", "Wooten", "inceptos@turpisegestas.ca"),
  Contact("Sebastian", "Watts", "Sed.diam.lorem@lorem.co.uk"),
  Contact("Chelsea", "Larsen", "ligula@Nam.net"),
  Contact("Cameron", "Humphrey", "placerat@id.org"),
  Contact("Juliet", "Bush", "consectetuer.euismod@vitaeeratVivamus.co.uk"),
  Contact("Caryn", "Hooper", "eu.enim.Etiam@ridiculus.org")
)
