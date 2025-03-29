package files.howtos

import utils.Bundle.*

object MatchMultiplePaths extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Match Multiple Paths")
    .withLabel("Match Multiple Paths")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to match on multiple paths?",
    s"""
    You can use the `|` operator in a pattern match:
    ```scala
    case GET -> (Path("hello") | Path("hello-world")) =>
      ...
    ```
    You can always check the [Scala docs](https://docs.scala-lang.org/scala3/book/control-structures.html#handling-multiple-possible-matches-on-one-line)
    for more help.

    ---
    If you want to handle all paths that start with "my-prefix/":
    ```scala
    case GET -> Path("my-prefix", segments*) =>
      ...
    ```

    ---
    If you want to handle all possible paths:
    ```scala
    case GET -> Path(segments*) =>
      ...
    ```

    """.md
  )
}
