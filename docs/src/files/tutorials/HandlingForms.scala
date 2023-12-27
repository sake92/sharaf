package files.tutorials

import utils.*
import Bundle.*

object HandlingForms extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("Handling Forms")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "Handling Form data",
    s"""
    All you have to do is make a `case class MyFormData(..) derives FormDataRW`  
    and then use it like this: `Request.current.bodyForm[MyFormData]`

    ---

    Create a file `form_handling.sc` and paste this code into it:
    ```scala
    //> using scala "3.3.1"
    //> using dep ba.sake::sharaf:${Consts.ArtifactVersion}

    import io.undertow.Undertow
    import ba.sake.formson.FormDataRW
    import ba.sake.hepek.html.HtmlPage
    import ba.sake.hepek.scalatags.all.*
    import ba.sake.sharaf.*, routing.*

    object ContacUsView extends HtmlPage:
      override def bodyContent =
        form(action := "/handle-form", method := "POST")(
          div(
            label("Full Name: ", input(name := "fullName", autofocus))
          ),
          div(
            label("Email: ", input(name := "email", tpe := "email"))
          ),
          input(tpe := "Submit")
        )

    case class ContactUsForm(fullName: String, email: String) derives FormDataRW

    val routes = Routes:
      case GET() -> Path() =>
        Response.withBody(ContacUsView)

      case POST() -> Path("handle-form") =>
        val formData = Request.current.bodyForm[ContactUsForm]
        Response.withBody(s"Got form data: $${formData}")

    Undertow.builder
      .addHttpListener(8181, "localhost")
      .setHandler(SharafHandler(routes))
      .build
      .start()

    println(s"Server started at http://localhost:8181")
    ```

    Then run it like this:
    ```sh
    scala-cli form_handling.sc 
    ```

    Now go to [http://localhost:8181](http://localhost:8181)
    and fill in the page with some data.

    When you click the "Submit" button you will see a response like this:
    ```
    Got form data: ContactUsForm(Bob,bob@example.com)
    ```
    """.md
  )
}
