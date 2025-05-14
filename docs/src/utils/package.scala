package utils

import scalatags.Text.all.*
import ba.sake.hepek.core.RelativePath
import ba.sake.hepek.html.statik

type Section = statik.Section
val Section = statik.Section

def pager(thisSp: statik.BlogPostPage)(using caller: RelativePath) = {

  def bsNavigation(navLinks: Frag*) = tag("nav")(
    ul(cls := "pagination justify-content-center")(navLinks)
  )

  val posts = thisSp.categoryPosts
  val indexOfThis = posts.indexOf(thisSp)
  if posts.length > 1 && indexOfThis >= 0 then {

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
