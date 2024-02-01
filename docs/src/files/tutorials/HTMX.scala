package files.tutorials

import utils.*
import Bundle.*

object HTMX extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("HTMX")

  override def blogSettings =
    super.blogSettings.withSections(htmxSection)

  val htmxSection = Section(
    "Using HTMX",
    s"""
    [HTMX]("https://htmx.org/") is an incredibly simple, HTML-first library.  
    Instead of going through HTML->JS->JSON-API loop/mess, you can go directly HTML->HTML-API.  
    Basically you just return HTML snippets that get included where you want in your page.

    Sharaf is using the [hepek-components](https://sake92.github.io/hepek/hepek/components/reference/bundle-reference.html)
    as its template engine, which has support for HTMX attributes.

    ---
    
    Let's make a simple page that triggers a POST request to fetch a HTML snippet.  
    Create a file `htmx_load_snippet.sc` and paste this code into it:
    ```scala
    ${ScalaCliFiles.htmx_load_snippet.indent(4)}
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
