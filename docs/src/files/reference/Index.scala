package files.reference

import scalatags.Text.all.*
import utils.*

object Index extends ReferencePage {

  override def pageSettings =
    super.pageSettings.withTitle("Reference")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    s"${Consts.ProjectName} reference",
    div(
      s"""
      
      Take a look at [Sharaf scaladoc](https://javadoc.io/doc/ba.sake/sharaf_3).
      """.md
    )
  )
}
