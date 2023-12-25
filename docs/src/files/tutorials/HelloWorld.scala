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
      Let's make a Hello World example in scala-cli.  
      Create a file `hello_sharaf.sc` and paste this code into it:
      ```scala
      //> using scala "3.3.1"
      //> using dep ba.sake::sharaf:${Consts.ArtifactVersion}

      import io.undertow.Undertow
      import ba.sake.sharaf.*, routing.*

      val routes = Routes:
        case GET() -> Path("hello", name) =>
          Response.withBody(s"Hello $$name")

      Undertow
        .builder
        .addHttpListener(8181, "localhost")
        .setHandler(SharafHandler(routes))
        .build
        .start()

      println(s"Server started at http://localhost:8181")
      ```

      Then run it like this:
      ```sh
      scala-cli hello_sharaf.sc 
      ```
      Go to [http://localhost:8181/hello/Bob](http://localhost:8181/hello/Bob).  
      You will see a "Hello Bob" text response.

      ---
      The most interesting part is the `Routes` definition.  
      Here we pattern match on `(HttpMethod, Path)`.  
      The `Path` contains a `Seq[String]`, which are the parts of the URL you can match on.
      """.md
    )
  )
}
