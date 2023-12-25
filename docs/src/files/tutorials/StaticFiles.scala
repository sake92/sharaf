package files.tutorials

import utils.*
import Bundle.*

object StaticFiles extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("Static Files")

  override def blogSettings =
    super.blogSettings.withSections(htmlSection)

  val htmlSection = Section(
    "Serving Static Files",
    s"""

    The static files are automatically served from the `resources/public` folder.  
    If using Mill, those are under `my_project/resources/public`.  
    In Sbt those are under `src/main/resources/public`.  
    In scala-cli you need to manually tell it where to look for with `--resource-dir resources`.
    
    ---
    
    Let's serve an `example.js` file with Sharaf.  
    First create a file `resources/public/example.js`.  
    Put this text into it: `console.log('Hello Sharaf!');`.

    Now create a file `static_files.sc` and paste this code into it:
    ```scala
    //> using scala "3.3.1"
    //> using dep ba.sake::sharaf:${Consts.ArtifactVersion}

    import io.undertow.Undertow
    import ba.sake.sharaf.*, routing.*

    val routes = Routes:
      case GET() -> Path() =>
        Response.withBody("Try /example.js")

    Undertow.builder
      .addHttpListener(8181, "localhost")
      .setHandler(SharafHandler(routes))
      .build
      .start()

    println(s"Server started at http://localhost:8181")
    ```

    and run it like this:
    ```sh
    scala-cli static_files.sc  --resource-dir resources
    ```

    Go to [http://localhost:8181/example.js](http://localhost:8181/example.js).  
    You will see the `example.js` contents served.

    """.md
  )
}
