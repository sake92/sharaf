package demo

import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.hepek.html.HtmlPage
import scalatags.Text.all

class AppRoutes(securityService: SecurityService) {

  val routes = Routes {

    case GET() -> Path("protected") =>
      Response.withBody(Views.ProtectedPage)

    case GET() -> Path("login") =>
      Response.redirect("/")

    case GET() -> Path() =>
      Response.withBody(Views.IndexPage(securityService.currentUser))

    case _ =>
      Response.withBody("Not found. ¯\\_(ツ)_/¯")
  }

}

object Views {

  import scalatags.Text.all.*

  def IndexPage(userOpt: Option[CustomUserProfile]): HtmlPage = new {
    override def pageContent: all.Frag = frag(
      userOpt match {
        case None =>
          frag(
            div("Hello there!"),
            div(
              // any protected route would work here actually..
              // just need to set ?provider=GitHubClient
              a(href := "/login?provider=GitHubClient")("Login with GitHub")
            )
          )
        case Some(user) =>
          frag(
            div(
              s"Hello ${user.name} !"
            ),
            div(
              a(href := "/protected")("Protected page")
            ),
            div(
              a(href := "/logout")("Logout")
            )
          )
      }
    )
  }

  val ProtectedPage: HtmlPage = new {
    override def pageContent: all.Frag = frag(
      div("This is a protected page"),
      div(
        a(href := "/")("Home")
      )
    )
  }
}
