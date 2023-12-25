package files.tutorials

import utils.*
import Bundle.*

object HTML extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("HTML")

  override def blogSettings =
    super.blogSettings.withSections(htmlSection)

  val htmlSection = Section(
    "Serving HTML",
    s"""

    Sharaf is using the [hepek-components](https://sake92.github.io/hepek/hepek/components/reference/bundle-reference.html)
    as its "template engine".  
    It is a bit different than other template engines, in the sense that it is *plain scala code*.  
    There is no separate language you need to learn.  
    It has useful utilities like Bootstrap 5 templates, form helpers etc. so you can focus on the important stuff.

    ---
    
    Let's make a simple HTML page that greets the user.  
    Create a file `html.sc` and paste this code into it:
    ```scala
    //> using scala "3.3.1"
    //> using dep ba.sake::sharaf:${Consts.ArtifactVersion}

    import io.undertow.Undertow
    import ba.sake.hepek.html.HtmlPage
    import ba.sake.hepek.scalatags.all.*
    import ba.sake.sharaf.*, routing.*

    class HelloView(name: String) extends HtmlPage:
      override def bodyContent =
        div("Hello ", b(name), "!")

    val routes = Routes:
      case GET() -> Path("hello", name) =>
        Response.withBody(HelloView(name))

    Undertow.builder
      .addHttpListener(8181, "localhost")
      .setHandler(SharafHandler(routes))
      .build
      .start()

    println(s"Server started at http://localhost:8181")
    ```

    and run it like this:
    ```sh
    scala-cli html.sc 
    ```

    Go to [http://localhost:8181/hello/Bob](http://localhost:8181/hello/Bob).  
    You will see a simple HTML page that greets the user.

    """.md
  )
}
