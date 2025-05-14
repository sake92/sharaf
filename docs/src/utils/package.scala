package utils

import scalatags.Text.all.*
import ba.sake.hepek.core.RelativePath
import ba.sake.hepek.html.statik

type Section = statik.Section
val Section = statik.Section

def pager(thisSp: statik.BlogPostPage)(using caller: RelativePath) = {

  def picoButtons(navLinks: Frag*) = tag("nav")(
    div(role := "group")(navLinks)
  )

  val posts = thisSp.categoryPosts
  val indexOfThis = posts.indexOf(thisSp)
  if posts.length > 1 && indexOfThis >= 0 then {
    if indexOfThis == 0 then
      picoButtons(
        a(href := "#", disabled, role := "button", cls := "outline")("Previous"),
        a(href := posts(indexOfThis + 1).ref, role := "button", cls := "outline")("Next")
      )
    else if indexOfThis == posts.length - 1 then
      picoButtons(
        a(href := posts(indexOfThis - 1).ref, role := "button", cls := "outline")("Previous"),
        a(href := "#", disabled, role := "button", cls := "outline")("Next")
      )
    else
      picoButtons(
        a(href := posts(indexOfThis - 1).ref, role := "button", cls := "outline")("Previous"),
        a(href := posts(indexOfThis + 1).ref, role := "button", cls := "outline")("Next")
      )
  } else frag()

}
