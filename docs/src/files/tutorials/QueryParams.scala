package files.tutorials

import utils.*

object QueryParams extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("Query Params")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "Query Parameters",
    s"""
    ### Raw
    Raw query parameters can be accessed through `Request.current.queryParamsRaw`.  
    This is a `Map[String, Seq[String]]` which you can use to extract query parameters.  
    The raw approach is useful for simple cases and dynamic query parameters.

    ### Typed
    For more type safety you can use the `QueryStringRW` typeclass.  
    Make a `case class MyParams(..) derives QueryStringRW`  
    and then use it like this: `Request.current.queryParams[MyParams]`

    ---

    Create a file `query_params.sc` and paste this code into it:
    ```scala
    ${ScalaCliFiles.query_params.indent(4)}
    ```

    Then run it like this:
    ```sh
    scala-cli query_params.sc 
    ```

    ---
    Now go to [http://localhost:8181/raw?q=what&perPage=10](http://localhost:8181/raw?q=what&perPage=10)
    and you will get the raw query params map:
    ```
    params = Map(perPage -> List(10), q -> List(what))
    ```

    and if you go to [http://localhost:8181/typed?q=what&perPage=10](http://localhost:8181/typed?q=what&perPage=10)
    you will get a type-safe, parsed query params object:
    ```
    params = SearchParams(what,10)
    ```
    """.md
  )

}
