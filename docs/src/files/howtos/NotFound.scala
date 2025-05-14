package files.howtos

import utils.*

object NotFound extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Customize NotFound Handler")
    .withLabel("Custom NotFound Handler")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to customize 404 NotFound handler?",
    s"""

    Use the `withNotFoundHandler` on `SharafHandler`:
    ```scala
    SharafHandler(routes).withNotFoundHandler { req =>
      Response.withBody(MyCustomNotFoundPage)
        .withStatus(StatusCodes.NOT_FOUND)
    }
    ```

    The `withNotFoundHandler` accepts a `Request => Response[?]` parameter.  
    You can use the request if you need to dynamically decide on what to return.  
    Or ignore it and return a static not found response.

    """.md
  )
}
