package userpassform

import ba.sake.sharaf.{*, given}
import ba.sake.querson.QueryStringRW
import ba.sake.sharaf.pac4j.SecurityService
import org.pac4j.core.profile.UserProfile

class AppRoutes(callbackUrl: String):
  val routes = Routes {
    case GET -> Path("login-form") =>
      case class QP(username: String = "", error: Option[String]) derives QueryStringRW
      val qp = Request.current.queryParams[QP]
      Response.withBody(views.showForm(callbackUrl, qp.error.nonEmpty, qp.username))
    case GET -> Path("protected-resource") =>
      SecurityService.currentUser match
        case None => Response.redirect("/login-form")
        case Some(user) =>
          Response.withBody(views.protectedResource(user.getUsername))
    case GET -> Path() =>
      Response.withBody(views.index(SecurityService.currentUser))
  }

object views:

  def index(currentUser: Option[UserProfile]) = {
    val currentUserHtml = currentUser.map { user =>
      html"""
        <div>
          Hello ${user.getUsername} !
          <div>
            <a href="/logout">Logout</a>
          </div>
        </div>
      """
    }
    html"""
      <!DOCTYPE html>
      <html>
      <body>
      <div>Hello there!</div>
      <div>
          <a href="/protected-resource">Protected resource</a>
          ${currentUserHtml}
      </div>
      </body>
      </html>
    """
  }

  def protectedResource(username: String) =
    html"""
      <!DOCTYPE html>
      <html>
      <body>
      <div>
          <a href="/">Home</a>
          <div>
          Hello ${username}! You are logged in!
          </div>
      </div>
      </body>
      </html>
    """

  def showForm(callbackUrl: String, isError: Boolean, username: String) =
    html"""
      <!DOCTYPE html>
      <html>
      <body>
      <div>
          <form action="${callbackUrl}?client_name=FormClient" method="POST">
            <label>Username
              <input type="text" name="username" value="${username}" required minlength="3">
            </label>
            <label>Password
              <input type="password" name="password" required minlength="8">
            </label>
            <input type="submit" value="Login">
          </form>
          ${if isError then html"<div style='background:orange'>Login failed, please try again.</div>" else ""}
          <div>
          Use johndoe/johndoe as username/password to login.
          </div>
      </div>
      </body>
      </html>
    """
