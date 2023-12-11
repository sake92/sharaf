package files

import utils.*
import Bundle.*, Tags.*

object Index extends SharafDocPage {

  override def pageSettings =
    super.pageSettings.withTitle("Hello world!")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "First section",
    div(
      Grid.row(
        s"""

        """.md
      )
    )
  )
}
