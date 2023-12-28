package utils

import ba.sake.hepek.core.RelativePath
import ba.sake.hepek.html.statik.BlogPostPage
import ba.sake.hepek.bootstrap5.statik.BootstrapStaticBundle

val Bundle = locally {
  val b = BootstrapStaticBundle.default
  import b.*

  val ratios = Ratios.default.withSingle(1, 2, 1).withHalf(1, 1).withThird(1, 2, 1)
  val grid = Grid.withScreenRatios(
    Grid.screenRatios.withSm(None).withXs(None).withLg(ratios).withMd(ratios)
  )
  b.withGrid(grid)
}

val FA = ba.sake.hepek.fontawesome5.FA

import Bundle.Tags.*

def pager(thisSp: BlogPostPage)(using caller: RelativePath) = {
  def bsNavigation(navLinks: Frag*) = tag("nav")(
    ul(cls := "pagination justify-content-center")(navLinks)
  )
  val posts = thisSp.categoryPosts
  if posts.length > 1 then {
    val indexOfThis = posts.indexOf(thisSp)
    if indexOfThis == 0 then
      bsNavigation(
        li(cls := "disabled page-item")(
          a(href := "#", cls := "page-link")("Previous")
        ),
        li(title := posts(indexOfThis + 1).pageSettings.label, cls := "page-item")(
          a(href := posts(indexOfThis + 1).ref, cls := "page-link")("Next")
        )
      )
    else if indexOfThis == posts.length - 1 then
      bsNavigation(
        li(title := posts(indexOfThis - 1).pageSettings.label, cls := "page-item")(
          a(href := posts(indexOfThis - 1).ref, cls := "page-link")("Previous")
        ),
        li(cls := "disabled page-item")(
          a(href := "#", cls := "page-link")("Next")
        )
      )
    else
      bsNavigation(
        li(title := posts(indexOfThis - 1).pageSettings.label, cls := "page-item")(
          a(href := posts(indexOfThis - 1).ref, cls := "page-link")("Previous")
        ),
        li(title := posts(indexOfThis + 1).pageSettings.label, cls := "page-item")(
          a(href := posts(indexOfThis + 1).ref, cls := "page-link")("Next")
        )
      )
  } else frag()

}
