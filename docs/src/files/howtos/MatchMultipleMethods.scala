package files.howtos

import utils.Bundle.*

object MatchMultipleMethods extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Match Multiple Methods")
    .withLabel("Match Multiple Methods")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to match on multiple methods?",
    s"""
    You can use the `|` operator in a pattern match:
    ```scala
    case (GET | POST) -> Path() =>
      ...
    ```
    You can always check the [Scala docs](https://docs.scala-lang.org/scala3/book/control-structures.html#handling-multiple-possible-matches-on-one-line)
    for more help.

    ---
    If you want to handle all possible methods, just don't use any extractors:
    ```scala
    case method -> Path() =>
      ...
    ```

    """.md
  )
}
