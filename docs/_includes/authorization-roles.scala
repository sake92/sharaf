//> using scala 3.9.0
//> using dep ba.sake::sharaf-undertow:0.20.0
//> using dep ba.sake::sharaf-pac4j:0.20.0
//> using dep org.pac4j:pac4j-http:6.5.8
//> using options -Yretain-trees

package authorization.roles

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

import ba.sake.sharaf.exceptions.RejectedException

enum Role:
  case Admin, Support

object RolesExample:
  // Development credentials only: each listed username uses the password "demo".
  private def config(): Config =
    val authenticator = new Authenticator:
      override def validate(ctx: CallContext, credentials: Credentials): Optional[Credentials] =
        credentials match
          case c: UsernamePasswordCredentials
              if Set("admin", "support", "both", "reader").contains(c.getUsername) && c.getPassword == "demo" =>
            val profile = new CommonProfile()
            profile.setId(c.getUsername)
            val roles = c.getUsername match
              case "admin"   => Set(Role.Admin)
              case "support" => Set(Role.Support)
              case "both"    => Set(Role.Admin, Role.Support)
              case _         => Set.empty[Role]
            roles.foreach(role => profile.addRole(role.toString))
            c.setUserProfile(profile)
            Optional.of(c)
          case _ => Optional.empty()
    val client = new DirectBasicAuthClient(authenticator)
    client.setName("basic")
    new Config(new Clients(client))

  def authenticated(next: SharafHandler): SharafHandler =
    val security = Pac4jSecurityConfig(config(), clients = "basic", sessionStore = NoOpSessionStore())
    Pac4jSecurityHandler(security, SharafHandler.exceptions(next, ExceptionMapper.json))

  // Call inside an authenticated route; an empty set never grants access.
  def authorizedAnyRole[A](roles: Set[Role])(action: => A): A =
    val allowed = SecurityService.profiles.exists { profile =>
      roles.exists(role => profile.getRoles.contains(role.toString))
    }
    if !allowed then throw RejectedException("Required role is missing")
    action

  def handler(): SharafHandler =
    val routes = Routes {
      case GET -> Path("reports") => authorizedAnyRole(Set(Role.Admin, Role.Support)) {
        Response.withBody("Report")
      }
      case GET -> Path("closed") => authorizedAnyRole(Set.empty) {
        Response.withBody("Must never be returned")
      }
    }
    authenticated(SharafHandler.routes(routes))

  def server(port: Int = 8181): UndertowSharafServer =
    UndertowSharafServer("localhost", port, handler())

  @main def runRoles(): Unit =
    val app = server()
    sys.addShutdownHook(app.stop())
    app.start()
    println("Server started at http://localhost:8181")
