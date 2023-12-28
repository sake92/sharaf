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

    Composing is quite useful.  
    You can make a common query params class and use it in multiple top-leve query params, or standalone.

    The first option is to set the parameter to `Option[T]`:
    ```scala
    case class PageQP(page: Int, size: Int) derives QueryStringRW
    case class MyQP(q: String, p: PageQP) derives QueryStringRW
    ```

    Sharaf is quite lenient when parsing the query parameters, so both combinations will work:
    - `?q=abc&p.page=0&p.size=10` -> object style
    - `?q=abc&p[page]=0&p[size]=10` -> brackets style
    - `?q=abc&p[page]=0&p.size=10` -> mixed style (dont)
    """.md
  )
}
