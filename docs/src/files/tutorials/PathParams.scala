package files.tutorials

import utils.*


object PathParams extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("Path Params")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "Path Parameters",
    s"""
    Path parameters can be extracted from the `Path(segments: Seq[String])` argument.

    Create a file `path_params.sc` and paste this code into it:
    ```scala
    ${ScalaCliFiles.path_params.indent(4)}
    ```
    
    Then run it like this:
    ```sh
    scala-cli path_params.sc 
    ```

    ---
    Now go to [http://localhost:8181/string/abc](http://localhost:8181/string/abc)
    and you will get the param returned: `string = abc`.
    
    When you go to [http://localhost:8181/int/123](http://localhost:8181/int/123),  
    Sharaf will *try to extract* an `Int` from the path parameter.  
    If it doesn't match, it will fall through, try the next route.
    
    """.md
  )

}
