package userpassform

import scalatags.Text.all.*
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*

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
  def index(currentUserOpt: Option[CustomUserProfile]) = doctype("html")(
    html(
      body(
        a(href := "/protected-resource")("Protected resource"),
        currentUserOpt.map { user =>
          div(
            s"Hello ${user.name} !",
            div(
              a(href := "/logout")("Logout")
            )
          )
        }
      )
    )
  )

  def protectedResource(using currentUser: CustomUserProfile) = doctype("html")(
    html(
      body(
        a(href := "/")("Home"),
        div(s"Hello ${currentUser.name}! You are logged in!")
      )
    )
  )

  def showForm(callbackUrl: String) = doctype("html")(
    html(
      body(
        form(action := s"${callbackUrl}?client_name=FormClient", method := "POST")(
          label(
            "Username",
            input(tpe := "text", name := "username")
          ),
          label(
            "Password",
            input(tpe := "text", name := "password")
          ),
          input(tpe := "submit", value := "Login")
        )
      ),
      div(
        "Use johndoe/johndoe as username/password to login."
      )
    )
  )

}
