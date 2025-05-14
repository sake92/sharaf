package files.howtos

import utils.*

object ExceptionHandler extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Customize Exception Handler")
    .withLabel("Custom Exception Handler")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to customize the Exception handler?",
    s"""

    Use the `withExceptionMapper` on `UndertowSharafServer`:
    ```scala
    val customExceptionMapper: ExceptionMapper = {
      case e: MyException =>
        val errorPage = MyErrorPage(e.getMessage())
        Response.withBody(errorPage)
            .withStatus(StatusCode.InternalServerError)
    }
    val finalExceptionMapper = customExceptionMapper.orElse(ExceptionMapper.default)
    val server = UndertowSharafServer(routes)
      .withExceptionMapper(finalExceptionMapper)
    ```

    The `ExceptionMapper` is a partial function from an exception to `Response`.  
    Here we need to chain our custom exception mapper before the default one.
    """.md
  )
}
