package files.howtos

import utils.Bundle.*

object EnumPathParam extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Bind Enum Path Parameter")
    .withLabel("Enum Path Parameter")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to bind path parameter as an enum?",
    s"""

    Sharaf needs a `FromPathParam[T]` instance for the `param[T]` extractor.  
    It can automatically derive an instance for singleton enums:

    ```scala
    enum Cloud derives FromPathParam:
      case aws, gcp, azure

    val routes = Routes:
      case GET -> Path("pricing", param[Cloud](cloud)) =>
        Response.withBody(s"cloud = $${cloud}")
    ```

    """.md
  )
}
