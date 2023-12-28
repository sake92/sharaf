package files.howtos

import utils.Bundle.*

object CompositeQueryParam extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Bind Composite Query Parameter")
    .withLabel("Composite Query Parameter")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to bind composite query parameter?",
    s"""
    You can make a common query params class and use it in multiple top-level query params, or standalone:
    ```scala
    case class PageQP(page: Int, size: Int) derives QueryStringRW
    case class MyQP(q: String, p: PageQP) derives QueryStringRW
    ```

    Sharaf is quite lenient when parsing the query parameters, so all these combinations will work:
    - `?q=abc&p.page=0&p.size=10` -> object style
    - `?q=abc&p[page]=0&p[size]=10` -> brackets style
    - `?q=abc&p[page]=0&p.size=10` -> mixed style (dont)
    """.md
  )
}
