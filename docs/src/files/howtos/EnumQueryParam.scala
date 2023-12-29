package files.howtos

import utils.Bundle.*

object EnumQueryParam extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Bind Enum Query Parameter")
    .withLabel("Enum Query Parameter")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to bind query parameter as an enum?",
    s"""

    Sharaf needs a `QueryStringRW[T]` instance for query params.  
    It can automatically derive an instance for singleton enums:

    ```scala
    enum Cloud derives QueryStringRW:
      case aws, gcp, azure

    case class MyQueryParams(
      cloud: Cloud
    ) derives QueryStringRW
    ```

    """.md
  )
}
