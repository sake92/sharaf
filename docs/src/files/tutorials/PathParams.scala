package files.tutorials

import utils.*
import Bundle.*

object PathParams extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("Path Params")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "Path Parameters",
    s"""
    Path parameters can be matched and extracted from the `Path(segments: Seq[String])` value.

    Create a file `path_params.sc` and paste this code into it:
    ```scala
    //> using scala "3.3.1"
    //> using dep ba.sake::sharaf:${Consts.ArtifactVersion}

    import io.undertow.Undertow
    import ba.sake.sharaf.*, routing.*

    val routes = Routes:
      case GET() -> Path("str", p) =>
        Response.withBody(s"str = $${p}")

      case GET() -> Path("int", param[Int](p)) =>
        Response.withBody(s"int = $${p}")

    Undertow.builder
      .addHttpListener(8181, "localhost")
      .setHandler(SharafHandler(routes))
      .build
      .start()

    println(s"Server started at http://localhost:8181")
    ```
    
    Then run it like this:
    ```sh
    scala-cli path_params.sc 
    ```

    Now go to [http://localhost:8181/str/abc](http://localhost:8181/str/abc)
    and you will get the param returned: `str = abc`.
    
    When you go to [http://localhost:8181/int/123](http://localhost:8181/int/123),  
    Sharaf will *try to extract* an `Int` from the path parameter.  
    If it doesn't match, it will fall through, try the next route.
    
    """.md
  )

}
