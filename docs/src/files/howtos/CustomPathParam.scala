package files.howtos

import utils.Bundle.*

object CustomPathParam extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Bind Custom Path Parameter")
    .withLabel("Custom Path Parameter")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to bind a custom path parameter?",
    s"""
    Sharaf needs a `FromPathParam[T]` instance available:
    ```scala
    import ba.sake.sharaf.routing.*
    
    given FromPathParam[MyType] with {
      def parse(str: String): Option[MyType] =
        parseMyType(str) // impl here
    }

    val routes = Routes:
      case GET -> Path("pricing", param[MyType](myType)) =>
        Response.withBody(s"myType = $${myType}")
    ```
    """.md
  )
}
