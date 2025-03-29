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

    The static files are automatically served from the `resources/public` folder (on the classpath):
    - in Mill those are under `my_project/resources/public`
    - in sbt those are under `src/main/resources/public`
    - in scala-cli you need to manually tell it where to look for with `--resource-dir resources`
    
    ---
    
    Let's serve an `example.js` file with Sharaf.  
    First create a file `resources/public/example.js`.  
    Put this text into it: `console.log('Hello Sharaf!');`.

    Now create a file `static_files.sc` and paste this code into it:
    ```scala
    ${ScalaCliFiles.static_files.indent(4)}
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
