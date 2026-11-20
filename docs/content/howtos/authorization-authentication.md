---
layout: howto.html
title: Require authentication
description: Require authentication in Sharaf
---

# {{ page.title }}

Use this pattern when every authenticated caller may access an endpoint and its body does not need application-user data.
Follow the [example prerequisites](/howtos/authorization.html#run-and-adapt-the-examples).

## Protect the handler

Define an application `authenticated(next: SharafHandler)` helper that constructs a single `Pac4jSecurityHandler`
around all routes. Register a pac4j `PathMatcher` to exempt the exact landing path `/` and the public branch `/public`.
Keep pac4j's security-header matchers in the matcher list. Routes remain in one normal `Routes` definition; there is no
manual handler dispatch or duplicated path matching.

The example uses `excludePath("/")` for an exact path and `excludeBranch("/public")` for `/public` and its descendants.
`/public/help` is public, while `/publicity` still requires authentication. Path exemptions apply to every HTTP method;
only the explicitly defined route/method combinations return content.

If your application intentionally protects only a prefix, `includePath("/api/")` applies security to paths starting
with `/api/`; it does not cover `/api` itself, and other paths bypass security. Use explicit public exemptions when the
application should require authentication by default.

The `/members` route returns fixed content. It never calls `SecurityService.currentUser`, resolves a domain user, or
introduces a user context parameter. Authentication establishes access without requiring identity in the route body.

## Run and check access

```sh
scala run examples/scala-cli/authorization/01_authenticated.scala --server=false
```

In another terminal:

```sh
curl -i http://localhost:8181/                       # 200 Public
curl -i http://localhost:8181/public/help            # 200 Public help
curl -i http://localhost:8181/publicity              # 401: neighboring prefix
curl -i http://localhost:8181/members                # 401; WWW-Authenticate: Basic ...
curl -i -u alice:demo http://localhost:8181/members  # 200 Members only
curl -i -u alice:wrong http://localhost:8181/members # 401
```

`bob:demo` is also valid. No session cookie is created. An authenticated request to an unknown path receives 404;
an anonymous request to it still requires authentication.

If a route needs to query user-owned data, continue with [application identity and ownership](/howtos/authorization-ownership.html).
For a broad role requirement, use [authorizedAnyRole](/howtos/authorization-roles.html).


## Run this example's integration tests

From the example folder, run the application source, its adjacent tests, and the shared HTTP fixture together:

```sh
cd examples/scala-cli/authorization
scala test 01_authenticated.scala 01_authenticated.test.scala HttpExampleSuite.test.scala --server=false
```

This starts and stops the real server on a free port. No other examples or manually started servers are needed.

## Complete runnable example

[Source]({{ site.data.project.gh.sourcesUrl }}/examples/scala-cli/authorization/01_authenticated.scala). The listing is synchronized from the tested source.

<!-- example:start examples/scala-cli/authorization/01_authenticated.scala -->
```scala
//> using scala 3.9.0
//> using dep ba.sake::sharaf-undertow:0.21.0
//> using dep ba.sake::sharaf-pac4j:0.21.0
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
```
<!-- example:end -->
