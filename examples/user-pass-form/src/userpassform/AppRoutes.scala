package userpassform

import ba.sake.sharaf.{*, given}

class AppRoutes(callbackUrl: String, securityService: SecurityService) {
  val routes = Routes {
    case GET -> Path("login-form") =>
      Response.withBody(views.showForm(callbackUrl))
    case GET -> Path("protected-resource") =>
      securityService.withCurrentUser {
        Response.withBody(views.protectedResource)
      }
    case GET -> Path() =>
      val view = views.index(securityService.currentUser)
      Response.withBody(view)
  }

}

object views {
  def index(currentUserOpt: Option[CustomUserProfile]) = {
    val currentUserHtml = currentUserOpt.map { user =>
      html"""
        <div>
          Hello ${user.name} !
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

  def protectedResource(using currentUser: CustomUserProfile) =
    html"""
      <!DOCTYPE html>
      <html>
      <body>
      <div>
          <a href="/">Home</a>
          <div>
          Hello ${currentUser.name}! You are logged in!
          </div>
      </div>
      </body>
      </html>
    """

  def showForm(callbackUrl: String) =
    html"""
      <!DOCTYPE html>
      <html>
      <body>
      <div>
          <form action="${callbackUrl}?client_name=FormClient" method="POST">
            <label>Username
              <input type="text" name="username">
            </label>
            <label>Password
              <input type="text" name="password">
            </label>
            <input type="submit" value="Login">
          </form>
          <div>
          Use johndoe/johndoe as username/password to login.
          </div>
      </div>
      </body>
      </html>
    """

}
