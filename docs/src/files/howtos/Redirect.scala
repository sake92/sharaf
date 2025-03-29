package files.howtos

import utils.Bundle.*

object Redirect extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Redirect")
    .withLabel("Redirect")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to redirect?",
    s"""
    Use the `Response.redirect` function:
    ```scala
    case GET -> Path("a-deprecated-route") =>
      Response.redirect("/this-other-place")
    ```
    
    This will redirect the request to "/this-other-place",  
    with status `301 MOVED_PERMANENTLY`

    """.md
  )
}
