package files.howtos

import utils.Bundle.*

object CustomQueryParam extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Bind Custom Query Parameter")
    .withLabel("Custom Query Parameter")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to bind a custom query parameter?",
    s"""
    When you want to handle a custom *scalar* value in query params,
    you need to implement a `QueryStringRW[T]` instance manually:
    ```scala
    import ba.sake.querson.*

    given QueryStringRW[MyType] with {
      override def write(path: String, value: MyType): QueryStringData =
        QueryStringRW[String].write(path, value.toString)

      override def parse(path: String, qsData: QueryStringData): MyType =
        val str = QueryStringRW[String].parse(path, qsData)
        Try(MyType.fromString(str)).toOption.getOrElse(typeError(path, "MyType", str))
    }

    private def typeError(path: String, tpe: String, value: Any): Nothing =
      throw ParsingException(ParseError(path, s"invalid $$tpe", Some(value)))
    ```

    Then you can use it:
    ```scala
    case class MyQueryParams(
      myType: MyType
    ) derives QueryStringRW
    ```
    
    ---
    Note that Sharaf can automatically derive an instance for [singleton enums](${EnumQueryParam.ref}).
    """.md
  )
}
