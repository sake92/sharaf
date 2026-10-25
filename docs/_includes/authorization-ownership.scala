//> using scala 3.9.0
//> using dep ba.sake::sharaf-undertow:0.20.0
//> using dep ba.sake::sharaf-pac4j:0.20.0
//> using dep org.pac4j:pac4j-http:6.5.8
//> using options -Yretain-trees

package authorization.ownership

import sttp.model.StatusCode
import java.util.Optional
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.context.CallContext
import org.pac4j.core.credentials.{Credentials, UsernamePasswordCredentials}
import org.pac4j.core.credentials.authenticator.Authenticator
import org.pac4j.core.profile.CommonProfile
import org.pac4j.http.client.direct.DirectBasicAuthClient
import ba.sake.sharaf.*
import ba.sake.sharaf.pac4j.*
import ba.sake.sharaf.session.NoOpSessionStore
import ba.sake.sharaf.undertow.UndertowSharafServer

import ba.sake.tupson.JsonRW
import ba.sake.sharaf.exceptions.{NotFoundException, RejectedException}

case class CurrentUser(id: String)
case class Document(id: String, ownerId: String, title: String) derives JsonRW
case class Rename(title: String) derives JsonRW

class Documents:
  // In-memory demonstration state; reset whenever the server is recreated.
  private var documents = Map("private" -> Document("private", "alice", "Original"))

  def read(id: String)(using user: CurrentUser): Document = synchronized {
    documents.get(id).filter(_.ownerId == user.id)
      .getOrElse(throw NotFoundException("Document"))
  }

  def rename(id: String, title: String)(using CurrentUser): Document = synchronized {
    val updated = read(id).copy(title = title)
    documents = documents.updated(id, updated)
    updated
  }

  def delete(id: String)(using CurrentUser): Unit = synchronized {
    read(id)
    documents = documents.removed(id)
  }

object OwnershipExample:
  private val users = Map("alice" -> CurrentUser("alice"), "bob" -> CurrentUser("bob"))

  // Development credentials only: each listed username uses the password "demo".
  private def config(): Config =
    val authenticator = new Authenticator:
      override def validate(ctx: CallContext, credentials: Credentials): Optional[Credentials] =
        credentials match
          case c: UsernamePasswordCredentials
              if Set("alice", "bob", "unlinked").contains(c.getUsername) && c.getPassword == "demo" =>
            val profile = new CommonProfile()
            profile.setId(c.getUsername)
            c.setUserProfile(profile)
            Optional.of(c)
          case _ => Optional.empty()
    val client = new DirectBasicAuthClient(authenticator)
    client.setName("basic")
    new Config(new Clients(client))

  def authenticated(next: SharafHandler): SharafHandler =
    val security = Pac4jSecurityConfig(config(), clients = "basic", sessionStore = NoOpSessionStore())
    // Map application exceptions before pac4j can wrap them.
    Pac4jSecurityHandler(security, SharafHandler.exceptions(next, ExceptionMapper.json))

  def withCurrentUser[A](action: CurrentUser ?=> A): A =
    val user = SecurityService.currentUser.flatMap(profile => users.get(profile.getId))
      .getOrElse(throw RejectedException("No application account"))
    action(using user)

  def handler(): SharafHandler =
    val documents = new Documents()
    val routes = Routes {
      case GET -> Path("documents", id) => withCurrentUser {
        Response.withBody(documents.read(id))
      }
      case PUT -> Path("documents", id) => withCurrentUser {
        val change = Request.current.bodyJson[Rename]
        Response.withBody(documents.rename(id, change.title))
      }
      case DELETE -> Path("documents", id) => withCurrentUser {
        documents.delete(id)
        Response.withStatus(StatusCode.NoContent)
      }
    }
    SharafHandler.exceptions(authenticated(SharafHandler.routes(routes)), ExceptionMapper.json)

  def server(port: Int = 8181): UndertowSharafServer =
    UndertowSharafServer("localhost", port, handler())

  @main def runOwnership(): Unit =
    val app = server()
    sys.addShutdownHook(app.stop())
    app.start()
    println("Server started at http://localhost:8181")
