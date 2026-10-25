//> using scala 3.9.0
//> using dep ba.sake::sharaf-undertow:0.20.0
//> using dep ba.sake::sharaf-pac4j:0.20.0
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
