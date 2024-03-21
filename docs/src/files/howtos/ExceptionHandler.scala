package files.howtos

import utils.Bundle.*

object ExceptionHandler extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Customize Exception Handler")
    .withLabel("Custom Exception Handler")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to customize Exception handler?",
    s"""

    Use the `withExceptionMapper` on `SharafHandler`:
    ```scala
    val customExceptionMapper: ExceptionMapper = {
      case e: MyException =>
        val errorPage = MyErrorPage(e.getMessage())
        Response.withBody(errorPage)
            .withStatus(StatusCodes.INTERNAL_SERVER_ERROR)
    }
    val finalExceptionMapper = customExceptionMapper.orElse(ExceptionMapper.default)
    val httpHandler = SharafHandler(routes)
      .withExceptionMapper(finalExceptionMapper)
    ```

    The `ExceptionMapper` is a partial function from an exception to `Response`.  
    Here we need to chain our custom error mapper before the default one.
    """.md
  )
}
