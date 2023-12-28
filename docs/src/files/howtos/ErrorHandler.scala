package files.howtos

import utils.Bundle.*

object ErrorHandler extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Customize Error Handler")
    .withLabel("Custom Error Handler")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to customize Error handler?",
    s"""

    Use the `withErrorMapper` on `SharafHandler`:
    ```scala
    val customErrorMapper: ErrorMapper = {
      case e: MyException =>
        val errorPage = MyErrorPage(e.getMessage())
        Response.withBody(errorPage)
            .withStatus(StatusCodes.INTERNAL_SERVER_ERROR)
    }
    val finalErrorMapper = customErrorMapper.orElse(ErrorMapper.default)
    val httpHandler = SharafHandler(routes)
      .withErrorMapper(finalErrorMapper)
    ```

    The `ErrorMapper` is a partial function from an exception to `Response`.  
    Here we need to chain our custom error mapper before the default one.
    """.md
  )
}
