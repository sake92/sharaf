package files.tutorials

import utils.*
import Bundle.*

object Index extends SharafDocPage {

  override def pageSettings =
    super.pageSettings.withTitle("Hello world!")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "First section",
    s"""
  
    """.md
  )
}
