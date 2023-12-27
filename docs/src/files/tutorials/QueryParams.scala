package files.tutorials

import utils.*
import Bundle.*

object QueryParams extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("Query Params")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "Query Parameters",
    s"""
    Raw query parameters can be accessed through `Request.current.queryParamsMap`.  
    This is a `Map[String, Seq[String]]` which you can use to extract query parameters.

    The `queryParamsMap` approach is useful for simple cases and dynamic query parameters.  
    For more type safety you can use `QueryStringRW` typeclass.  
    All you have to do is make a `case class MyParams(..) derives QueryStringRW`  
    and then use it like this: `Request.current.queryParams[MyParams]`

    ---

    Create a file `query_params.sc` and paste this code into it:
    ```scala
    //> using scala "3.3.1"
    //> using dep ba.sake::sharaf:${Consts.ArtifactVersion}

    import io.undertow.Undertow
    import ba.sake.querson.QueryStringRW
    import ba.sake.sharaf.*, routing.*

    case class SearchParams(q: String, perPage: Int) derives QueryStringRW

    val routes = Routes:
      case GET() -> Path("raw") =>
        val qp = Request.current.queryParamsMap
        Response.withBody(s"params = $${qp}")

      case GET() -> Path("typed") =>
        val qp = Request.current.queryParams[SearchParams]
        Response.withBody(s"params = $${qp}")

    Undertow.builder
      .addHttpListener(8181, "localhost")
      .setHandler(SharafHandler(routes))
      .build
      .start()

    println(s"Server started at http://localhost:8181")
    ```

    Then run it like this:
    ```sh
    scala-cli query_params.sc 
    ```

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
