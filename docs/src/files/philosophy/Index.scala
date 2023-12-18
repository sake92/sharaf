package files.philosophy

import utils.Bundle.*
import utils.Consts

object Index extends PhilosophyPage {

  override def pageSettings =
    super.pageSettings.withTitle("Philosophy")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "Why not xyz?",
    s"""
    ...

    """.md
  )
}
