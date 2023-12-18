package files.howtos

import utils.Bundle.*
import utils.Consts

object Index extends HowToPage {

  override def pageSettings =
    super.pageSettings.withTitle("How-Tos")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How-Tos",
    s"""

    Here are some common questions and answers you might have when using ${Consts.ProjectName}.
    """.md
  )
}
