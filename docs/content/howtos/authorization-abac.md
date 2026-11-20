---
layout: howto.html
title: Implement an ABAC policy
description: Implement an ABAC policy in Sharaf
---

# {{ page.title }}

Use attribute-based access control when permission depends on several facts about the actor and the resource.
Follow the [example prerequisites](/howtos/authorization.html#run-and-adapt-the-examples).

## Define a focused policy

For editing a document, require an active account, an active subscription, ownership, and an unarchived document:

```scala
def canEdit(user: CurrentUser, document: Document): Boolean =
  user.active && user.subscribed && document.ownerId == user.id && !document.archived
```

Resolve account attributes from trusted application state and load resource attributes from storage.
Do not take `active`, `subscribed`, `ownerId`, or `archived` from the edit request.
If your policy also depends on environment facts such as time, obtain them from a trusted source at the operation boundary.

In `Documents.rename`, call `EditPolicy.requireCanEdit(user, document)` before changing stored state. Use the same predicate for any displayed edit capability,
while keeping the service check decisive. Keep the check and write consistent through synchronization in this demo or
an appropriate database transaction/conditional write in an application.

The read policy is separate: an owner may inspect their document even when editing is unavailable.
This lets the API show that rejected edits did not change stored state.

## Run and check each condition

```sh
scala run examples/scala-cli/authorization/04_abac.scala --server=false
```

```sh
curl -i -u alice:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Updated"}' http://localhost:8181/documents/draft # 200
curl -i -u bob:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Denied"}' http://localhost:8181/documents/draft # 403: another owner
curl -i -u trial:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Denied"}' http://localhost:8181/documents/trial # 403: no subscription
curl -i -u inactive:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Denied"}' http://localhost:8181/documents/inactive # 403: inactive account
curl -i -u alice:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Denied"}' http://localhost:8181/documents/archived # 403: archived
curl -i -u alice:demo http://localhost:8181/documents/archived # 200; still Archived
```

The API suite varies each decisive condition independently and sends forged attributes to verify they cannot grant access.
For decisions involving course membership, add [scoped relationship checks](/howtos/authorization-relationships.html).


## Run this example's integration tests

From the example folder, run the application source, its adjacent tests, and the shared HTTP fixture together:

```sh
cd examples/scala-cli/authorization
scala test 04_abac.scala 04_abac.test.scala HttpExampleSuite.test.scala --server=false
```

This starts and stops the real server on a free port. No other examples or manually started servers are needed.

## Complete runnable example

[Source]({{ site.data.project.gh.sourcesUrl }}/examples/scala-cli/authorization/04_abac.scala). The listing is synchronized from the tested source.

<!-- example:start examples/scala-cli/authorization/04_abac.scala -->
```scala
//> using scala 3.9.0
//> using dep ba.sake::sharaf-undertow:0.21.0
//> using dep ba.sake::sharaf-pac4j:0.21.0
//> using dep org.pac4j:pac4j-http:6.5.8
//> using options -Yretain-trees

package authorization.abac

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

case class CurrentUser(id: String, active: Boolean, subscribed: Boolean)
case class Document(id: String, ownerId: String, title: String, archived: Boolean) derives JsonRW
case class Rename(title: String) derives JsonRW

object EditPolicy:
  def canEdit(user: CurrentUser, document: Document): Boolean =
    user.active && user.subscribed && document.ownerId == user.id && !document.archived

  def requireCanEdit(user: CurrentUser, document: Document): Unit =
    if !canEdit(user, document) then throw RejectedException("Editing is not allowed")

class Documents:
  private var documents = Map(
    "draft" -> Document("draft", "alice", "Draft", false),
    "archived" -> Document("archived", "alice", "Archived", true),
    "trial" -> Document("trial", "trial", "Trial", false),
    "inactive" -> Document("inactive", "inactive", "Inactive", false)
  )

  def read(id: String)(using user: CurrentUser): Document = synchronized {
    documents.get(id).filter(_.ownerId == user.id)
      .getOrElse(throw NotFoundException("Document"))
  }

  def rename(id: String, title: String)(using user: CurrentUser): Document = synchronized {
    val document = documents.getOrElse(id, throw NotFoundException("Document"))
    EditPolicy.requireCanEdit(user, document)
    val updated = document.copy(title = title)
    documents = documents.updated(id, updated)
    updated
  }

object AbacExample:
  private val users = Map(
    "alice" -> CurrentUser("alice", active = true, subscribed = true),
    "bob" -> CurrentUser("bob", active = true, subscribed = true),
    "trial" -> CurrentUser("trial", active = true, subscribed = false),
    "inactive" -> CurrentUser("inactive", active = false, subscribed = true)
  )

  // Development credentials only: each listed username uses the password "demo".
  private def config(): Config =
    val authenticator = new Authenticator:
      override def validate(ctx: CallContext, credentials: Credentials): Optional[Credentials] =
        credentials match
          case c: UsernamePasswordCredentials
              if Set("alice", "bob", "trial", "inactive").contains(c.getUsername) && c.getPassword == "demo" =>
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
    }
    SharafHandler.exceptions(authenticated(SharafHandler.routes(routes)), ExceptionMapper.json)

  def server(port: Int = 8181): UndertowSharafServer =
    UndertowSharafServer("localhost", port, handler())

  @main def runAbac(): Unit =
    val app = server()
    sys.addShutdownHook(app.stop())
    app.start()
    println("Server started at http://localhost:8181")
```
<!-- example:end -->
