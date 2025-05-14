package files.tutorials

import utils.*
import Bundle.*

object JsonAPI extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("JSON API")

  override def blogSettings =
    super.blogSettings.withSections(modelSection, routesSection, runSection)

  private val snip1 = ScalaCliFiles.json_api.snippet(until = "val routes").indent(4)
  
  private val snip2 = ScalaCliFiles.json_api
    .snippet(from = "val routes", until = "UndertowSharafServer")
    .indent(4)
    .trim
  
  private val snip3 = ScalaCliFiles.json_api.snippet(from = "UndertowSharafServer").indent(4)

  val modelSection = Section(
    "Model definition",
    s"""
    Let's make a simple JSON API in scala-cli.  
    Create a file `json_api.sc` and paste this code into it:
    ```scala
    ${snip1}
    ```

    Here we defined a `Car` model, which `derives JsonRW`, so we can use the JSON support from Sharaf.

    We also use a `var db: Seq[Car]` to store our data.  
    (don't do this for real projects)
    """.md
  )

  val routesSection = Section(
    "Routes definition",
    s"""
    Next step is to define a few routes for getting and adding cars:
    ```scala
    ${snip2}
    ```

    The first route returns all data in the database.  
    
    The second route does some filtering on the database.  
    
    The third route binds the JSON body from the HTTP request.  
    Then we add it to the database.
    """.md
  )

  val runSection = Section(
    "Running the server",
    s"""
    Finally, start up the server:
    ```scala
    ${snip3}
    ```

    and run it like this:
    ```sh
    scala-cli json_api.sc 
    ```

    Then try the following requests:
    ```sh
    # get all cars
    curl http://localhost:8181/cars

    # add a car
    curl --request POST \\
      --url http://localhost:8181/cars \\
      --data '{
        "brand": "Mercedes",
        "model": "ML350",
        "quantity": 1
      }'

    # get cars by brand
    curl http://localhost:8181/cars/Mercedes
    ```
    """.md
  )
}
