package files.tutorials

import utils.*
import Bundle.*, Tags.*

object FirstTutorial extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("First Tutorial")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "First Tutorial",
    div(
      s"""
      Let's start with a simple tutorial..
      """.md,
    )
  )
}
