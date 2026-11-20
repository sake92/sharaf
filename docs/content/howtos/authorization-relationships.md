---
layout: howto.html
title: Authorize scoped relationships
description: Authorize scoped relationships in Sharaf
---

# {{ page.title }}

Use relationship-based checks when access depends on how the caller relates to a particular course, project, or tenant.
Follow the [example prerequisites](/howtos/authorization.html#run-and-adapt-the-examples).

## Model memberships as independent facts

Keep actor identity separate from memberships and subscription attributes. Store a membership's user, target course,
and set of course roles together. A person can be both teacher and student in one course, and only a student in another.
Do not model these combinations as mutually exclusive subclasses of the user.

## Check the target resource in the service

Look up membership using **both the current user's ID and the requested course ID**. Require that membership for reads.
For edits, require a teacher relationship to that course and require the course to be unarchived.
The example policy also checks that membership and course IDs agree and that the membership belongs to the actor.
`Courses.rename` calls `CoursePolicy.requireCanTeach(user, course, member)` before changing stored state. A denied
policy check throws `RejectedException`, mapped to 403 inside the pac4j boundary.

Resolve memberships from trusted storage. Do not accept a role, membership scope, or actor ID from the rename request.
If you pass membership through a context function, validate its scope against the actual resource at the enforcement point.
A context describing course A must never authorize a write to course B.

Return 404 for nonmembers and 403 for members who lack editing permission. Continue to enforce the policy inside the
service even if the UI hides the edit button.

## Run and check combined roles and scope

```sh
scala run examples/scala-cli/authorization/05_relationships.scala --server=false
```

```sh
curl -i -u alice:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Revised A"}' http://localhost:8181/courses/a # 200: teacher AND student
curl -i -u bob:demo http://localhost:8181/courses/a # 200: student may read
curl -i -u bob:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Denied"}' http://localhost:8181/courses/a # 403: student only here
curl -i -u alice:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Denied","courseId":"a","roles":["Teacher"]}' http://localhost:8181/courses/b # 403
curl -i -u bob:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Revised B"}' http://localhost:8181/courses/b # 200: teacher in B
curl -i -u alice:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Denied"}' http://localhost:8181/courses/archived # 403: archived
curl -i -u outsider:demo http://localhost:8181/courses/a # 404: no membership
```

The policy combines a relationship (teacher of this course) with a resource attribute (not archived).
For the distinction between the access-control models, see [the explanation](/philosophy/authorization.html).


## Run this example's integration tests

From the example folder, run the application source, its adjacent tests, and the shared HTTP fixture together:

```sh
cd examples/scala-cli/authorization
scala test 05_relationships.scala 05_relationships.test.scala HttpExampleSuite.test.scala --server=false
```

This starts and stops the real server on a free port. No other examples or manually started servers are needed.

## Complete runnable example

[Source]({{ site.data.project.gh.sourcesUrl }}/examples/scala-cli/authorization/05_relationships.scala). The listing is synchronized from the tested source.

<!-- example:start examples/scala-cli/authorization/05_relationships.scala -->
```scala
//> using scala 3.9.0
//> using dep ba.sake::sharaf-undertow:0.21.0
//> using dep ba.sake::sharaf-pac4j:0.21.0
//> using dep org.pac4j:pac4j-http:6.5.8
//> using options -Yretain-trees

package authorization.relationships

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

enum CourseRole:
  case Teacher, Student

case class CurrentUser(id: String)
case class Course(id: String, title: String, archived: Boolean) derives JsonRW
case class Membership(userId: String, courseId: String, roles: Set[CourseRole])
case class Rename(title: String) derives JsonRW

object CoursePolicy:
  def canTeach(user: CurrentUser, course: Course, membership: Membership): Boolean =
    membership.userId == user.id && membership.courseId == course.id &&
      membership.roles.contains(CourseRole.Teacher) && !course.archived

  def requireCanTeach(user: CurrentUser, course: Course, membership: Membership): Unit =
    if !canTeach(user, course, membership) then throw RejectedException("Teaching access required")

class Courses:
  private val memberships = List(
    Membership("alice", "a", Set(CourseRole.Teacher, CourseRole.Student)),
    Membership("alice", "b", Set(CourseRole.Student)),
    Membership("bob", "a", Set(CourseRole.Student)),
    Membership("bob", "b", Set(CourseRole.Teacher)),
    Membership("alice", "archived", Set(CourseRole.Teacher))
  )
  private var courses = Map(
    "a" -> Course("a", "Course A", false),
    "b" -> Course("b", "Course B", false),
    "archived" -> Course("archived", "Archived", true)
  )

  private def membership(id: String)(using user: CurrentUser): Membership =
    memberships.find(m => m.userId == user.id && m.courseId == id)
      .getOrElse(throw NotFoundException("Course"))

  def read(id: String)(using CurrentUser): Course = synchronized {
    membership(id)
    courses.getOrElse(id, throw NotFoundException("Course"))
  }

  def rename(id: String, title: String)(using user: CurrentUser): Course = synchronized {
    val course = read(id)
    val member = membership(id)
    CoursePolicy.requireCanTeach(user, course, member)
    val updated = course.copy(title = title)
    courses = courses.updated(id, updated)
    updated
  }

object RelationshipsExample:
  private val users = Map(
    "alice" -> CurrentUser("alice"),
    "bob" -> CurrentUser("bob"),
    "outsider" -> CurrentUser("outsider")
  )

  // Development credentials only: each listed username uses the password "demo".
  private def config(): Config =
    val authenticator = new Authenticator:
      override def validate(ctx: CallContext, credentials: Credentials): Optional[Credentials] =
        credentials match
          case c: UsernamePasswordCredentials
              if Set("alice", "bob", "outsider").contains(c.getUsername) && c.getPassword == "demo" =>
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
    val courses = new Courses()
    val routes = Routes {
      case GET -> Path("courses", id) => withCurrentUser {
        Response.withBody(courses.read(id))
      }
      case PUT -> Path("courses", id) => withCurrentUser {
        val change = Request.current.bodyJson[Rename]
        Response.withBody(courses.rename(id, change.title))
      }
    }
    SharafHandler.exceptions(authenticated(SharafHandler.routes(routes)), ExceptionMapper.json)

  def server(port: Int = 8181): UndertowSharafServer =
    UndertowSharafServer("localhost", port, handler())

  @main def runRelationships(): Unit =
    val app = server()
    sys.addShutdownHook(app.stop())
    app.start()
    println("Server started at http://localhost:8181")
```
<!-- example:end -->
