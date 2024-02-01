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
    as its template engine.  
    Hepek is a bit different than other template engines, in the sense that it is *plain scala code*.  
    There is no separate language you need to learn.  
    It has useful utilities like Bootstrap 5 templates, form helpers etc. so you can focus on the important stuff.

    ---
    
    Let's make a simple HTML page that greets the user.  
    Create a file `html.sc` and paste this code into it:
    ```scala
    ${ScalaCliFiles.html}
    ```

    and run it like this:
    ```sh
    scala-cli html.sc 
    ```

    Go to [http://localhost:8181](http://localhost:8181)  
    to see how it works.

    """.md
  )
}
