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
      ...

      ```scala
      println("Hello!")
      ```
      """.md
    )
  )
}
