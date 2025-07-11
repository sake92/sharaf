package demo

import ba.sake.sharaf.{*, given}

class AppRoutes(securityService: SecurityService) {

  val routes = Routes:
    case GET -> Path("protected") =>
      Response.withBody(ProtectedPage)
    case GET -> Path("login") =>
      Response.redirect("/")
    case GET -> Path() =>
      Response.withBody(IndexPage(securityService.currentUser))
    case _ =>
      Response.withBody("Not found. ¯\\_(ツ)_/¯")

}

def IndexPage(userOpt: Option[CustomUserProfile]) =
  userOpt match {
    case None =>
      html"""
        <!DOCTYPE html>
        <html>
        <body>
        <div>Hello there!</div>
        <div>
            <a href="/login?force_client=GitHubClient">Login with GitHub</a>
        </div>
        </body>
        </html>
      """
    case Some(user) =>
      html"""
        <!DOCTYPE html>
        <html>
        <body>
        <div>Hello ${user.name} !</div>
        <div>
            <a href="/protected">Protected page</a>
        </div>
        <div>
            <a href="/logout">Logout</a>
        </div>
        </body>
        </html>
  """
  }

def ProtectedPage =
  html"""
    <!DOCTYPE html>
    <html>
    <body>
    <div>This is a protected page. You must be logged in to see this.</div>
    <div>
        <a href="/">Home</a>
    </div>
    </body>
    </html>
  """
