package files

import scalatags.Text.all.*
import ba.sake.hepek.html.statik.BlogPostPage
import utils.*

object Index extends DocStaticPage {

  override def pageSettings = super.pageSettings
    .withTitle(Consts.ProjectName)

  override def mainContent =
    div(
      h1(Consts.ProjectName),
      s"""
      ${Consts.ProjectName} is a minimalistic Scala 3 web framework.
  
      Jump right into:
      - [Tutorials](${files.tutorials.Index.ref}) to get you started
      - [How-Tos](${files.howtos.Index.ref}) to get answers for some common questions
      - [Reference](${files.reference.Index.ref}) to see detailed information
      - [Philosophy](${files.philosophy.Index.ref}) to get insights into design decisions
  
      ---
      Site map:
      """.md,
      div(cls := "site-map")(
        siteMap.md
      )
    )

  private def siteMap =
    Index.staticSiteSettings.mainPages
      .map {
        case mp: BlogPostPage =>
          val subPages = mp.categoryPosts
            .drop(1) // skip Index ..
            .map { cp =>
              s"  - [${cp.pageSettings.label}](${cp.ref})"
            }
            .mkString("\n")
          s"- [${mp.pageSettings.label}](${mp.ref})\n" + subPages
        case _ => ""
      }
      .mkString("\n")
}
