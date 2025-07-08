package demo

import scalatags.Text.all.*
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.hepek.html.HtmlPage
import ba.sake.sharaf.undertow.given

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

class IndexPage(userOpt: Option[CustomUserProfile]) extends HtmlPage {
  override def pageContent = frag(
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

object ProtectedPage extends HtmlPage {
  override def pageContent = frag(
    div("This is a protected page"),
    div(
      a(href := "/")("Home")
    )
  )
}
