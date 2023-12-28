package files.howtos

import utils.Bundle.*

object OptionalQueryParam extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Bind Optional Query Parameter")
    .withLabel("Optional Query Parameter")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to bind optional query parameter?",
    s"""

    The first option is to set the parameter to `Option[T]`:
    ```scala
    case class MyQP(mandatory: String, opt: Option[Int]) derives QueryStringRW
    ```
    If you make a request with params `?mandatory=abc`, `opt` will have value of `None`.

    ---
    The second option is to set the parameter to some default value:
    ```scala
    case class MyQP2(mandatory: String, opt: Int = 42) derives QueryStringRW
    ```
    Here if you make a request with params `?mandatory=abc` the `opt` will have value of `42`.

    > Note that you need the `-Yretain-trees` scalac flag turned on, otherwise it won't work!

    """.md
  )
}
