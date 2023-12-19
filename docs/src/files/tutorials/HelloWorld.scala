package files.tutorials

import utils.*
import Bundle.*, Tags.*

object HelloWorld extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("Hello World")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "Hello World",
    div(
      s"""
      Let's make a quick Hello World example in scala-cli.  
      Create a file `hello_sharaf.sc` and paste this code into it:
      ```scala
      //> using dep ba.sake::sharaf:${Consts.ArtifactVersion}

      import io.undertow.Undertow
      import ba.sake.sharaf.*, routing.*

      val routes = Routes:
        case GET() -> Path("hello", name) =>
          Response.withBody(s"Hello $$name")

      val server = Undertow
        .builder()
        .addHttpListener(8181, "localhost")
        .setHandler(SharafHandler(routes))
        .build()

      server.start()

      println(s"Server started at http://localhost:8181")
      ```

      Then run it like this:
      ```sh
      scala-cli hello_sharaf.sc 
      ```
      Then you can go to [http://localhost:8181/hello/Bob](http://localhost:8181/hello/Bob)  
      to try it out.
      """.md,
    )
  )
}
