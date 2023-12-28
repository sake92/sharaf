package files.howtos

import utils.Bundle.*

object RegexPathParam extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Bind Regex Path Parameter")
    .withLabel("Regex Path Parameter")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to bind path parameter as a regex?",
    s"""

    ```scala
    val userIdRegex = "user_id_(\\d+)".r

    val routes = Routes:
      case GET() -> Path("pricing", userIdRegex(userId)) =>
        Response.withBody(s"userId = $${userId}")
    ```

    Note that the `userId` is bound as a `String`.  
    You could further match on it,  
    for example `userIdRegex(param[Int](userId))` would extract it as an `Int`.
    """.md
  )
}
