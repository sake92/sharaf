//> using scala 3.9.0
//> using dep ba.sake::sharaf-undertow:0.20.0
//> using dep ba.sake::sharaf-pac4j:0.20.0
//> using dep org.pac4j:pac4j-http:6.5.8
//> using options -Yretain-trees

package authorization.authentication

import java.util.Optional
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.context.CallContext
import org.pac4j.core.credentials.{Credentials, UsernamePasswordCredentials}
import org.pac4j.core.credentials.authenticator.Authenticator
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.matching.matcher.{DefaultMatchers, PathMatcher}
import org.pac4j.http.client.direct.DirectBasicAuthClient
import ba.sake.sharaf.*
import ba.sake.sharaf.pac4j.*
import ba.sake.sharaf.session.NoOpSessionStore
import ba.sake.sharaf.undertow.UndertowSharafServer

object AuthenticationExample:
  // Development credentials only: each listed username uses the password "demo".
  private def config(): Config =
    val authenticator = new Authenticator:
      override def validate(ctx: CallContext, credentials: Credentials): Optional[Credentials] =
        credentials match
          case c: UsernamePasswordCredentials
              if Set("alice", "bob").contains(c.getUsername) && c.getPassword == "demo" =>
            val profile = new CommonProfile()
            profile.setId(c.getUsername)
            c.setUserProfile(profile)
            Optional.of(c)
          case _ => Optional.empty()
    val client = new DirectBasicAuthClient(authenticator)
    client.setName("basic")
    new Config(new Clients(client))

  def authenticated(next: SharafHandler): SharafHandler =
    val pac4j = config()
    val publicPaths = new PathMatcher()
    publicPaths.excludePath("/")
    publicPaths.excludeBranch("/public")
    pac4j.addMatcher("publicPaths", publicPaths)
    val security = Pac4jSecurityConfig(
      pac4j, clients = "basic", matchers = s"${DefaultMatchers.SECURITYHEADERS},publicPaths",
      sessionStore = NoOpSessionStore()
    )
    Pac4jSecurityHandler(security, next)

  def handler(): SharafHandler =
    val routes = Routes {
      case GET -> Path()                 => Response.withBody("Public")
      case GET -> Path("public", "help") => Response.withBody("Public help")
      case GET -> Path("members")        => Response.withBody("Members only")
    }
    authenticated(SharafHandler.routes(routes))

  def server(port: Int = 8181): UndertowSharafServer =
    UndertowSharafServer("localhost", port, handler())

  @main def runAuthentication(): Unit =
    val app = server()
    sys.addShutdownHook(app.stop())
    app.start()
    println("Server started at http://localhost:8181")
