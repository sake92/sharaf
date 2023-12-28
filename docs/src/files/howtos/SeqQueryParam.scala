package files.howtos

import utils.Bundle.*

object SeqQueryParam extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Bind Sequence Query Parameter")
    .withLabel("Sequence Query Parameter")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to bind sequence query parameter?",
    s"""

    Set the parameter to `Seq[T]`:
    ```scala
    case class MyQP(seq: Seq[Int]) derives QueryStringRW
    ```

    Let's consider a few possible requests with these query params:
    - `?` (empty) -> `seq` will be empty `Seq()`
    - `?seq=123` -> `seq` will be empty `Seq(123)`
    - `?seq[]=123&seq[]=456` -> `seq` will be empty `Seq(123, 456)`
    - `?seq[1]=123&seq[0]=456` -> `seq` will be empty `Seq(456, 123)` (note it is sorted here)
    """.md
  )
}
