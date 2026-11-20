---
layout: howto.html
title: Enforce resource ownership
description: Enforce resource ownership in Sharaf
---

# {{ page.title }}

Use this pattern when a route needs the caller's application identity and must protect a particular stored resource.
Follow the [example prerequisites](/howtos/authorization.html#run-and-adapt-the-examples).

## Resolve application identity at the boundary

After pac4j authenticates the request, map its trusted profile ID to the application's `CurrentUser`.
For multiple providers, include the provider/issuer in that mapping; do not assume IDs from different providers are unique.
Keep account lookup in this boundary code and keep pac4j profile types out of the service.

Use `withCurrentUser[A](action: CurrentUser ?=> A)` when several calls need the same identity. The example resolves the
user once per call and supplies it to service methods with `(using CurrentUser)`. Explicit user parameters are equally
appropriate for short call chains. Keep either form local to the operation, and resolve a fresh identity for each request.

A context parameter establishes which actor is available. The service must still check ownership for each resource.
A valid provider identity without a linked application account is denied with 403 in this example.

## Check stored ownership inside the service

Allow only the owner to read, rename, or delete a document. Read the owner from stored state and expose only `title`
in the rename request model. Do not accept owner changes as part of this operation.

The example uses one owner-checked read inside each synchronized write. In a database, use an appropriate transaction
or constrain the mutation by both document ID and owner ID, checking the result. Keep the service operation as the
enforcement boundary so REST, HTML, and future callers cannot bypass it.

Map `NotFoundException` to 404 for both a missing document and a private document belonging to someone else.
Place the application exception mapper [inside pac4j](/howtos/authorization.html#preserve-denial-responses).

## Run and check ownership

```sh
scala run examples/scala-cli/authorization/03_ownership.scala --server=false
```

```sh
curl -i -u alice:demo http://localhost:8181/documents/private # 200; Original, owned by alice
curl -i -u bob:demo http://localhost:8181/documents/private   # 404
curl -i -u bob:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Stolen","ownerId":"bob"}' http://localhost:8181/documents/private # 404
curl -i -u alice:demo http://localhost:8181/documents/private # unchanged
curl -i -u alice:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Renamed"}' http://localhost:8181/documents/private # 200; Renamed
curl -i -u bob:demo -X DELETE http://localhost:8181/documents/private   # 404
curl -i -u alice:demo -X DELETE http://localhost:8181/documents/private # 204
curl -i -u alice:demo http://localhost:8181/documents/private # 404 after deletion
```

`unlinked:demo` authenticates with pac4j but has no application account, so it receives 403.
Restart the server to reset the demonstration document.


## Run this example's integration tests

From the example folder, run the application source, its adjacent tests, and the shared HTTP fixture together:

```sh
cd examples/scala-cli/authorization
scala test 03_ownership.scala 03_ownership.test.scala HttpExampleSuite.test.scala --server=false
```

This starts and stops the real server on a free port. No other examples or manually started servers are needed.

## Complete runnable example

[Source]({{ site.data.project.gh.sourcesUrl }}/examples/scala-cli/authorization/03_ownership.scala). The listing is synchronized from the tested source.

<!-- example:start examples/scala-cli/authorization/03_ownership.scala -->
```scala
//> using scala 3.9.0
//> using dep ba.sake::sharaf-undertow:0.21.0
//> using dep ba.sake::sharaf-pac4j:0.21.0
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
```
<!-- example:end -->
