package files.tutorials

import scalatags.Text.all.*
import utils.*

object Tests extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("Tests")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "Tests",
    div(
      s"""
      Tests are essential to any serious software component.  
      Writing integration tests with Munit and Requests is straightforward.

      Here we are testing the API from the [JSON API tutorial](${JsonAPI.routesSection.ref}).  
      Create a file `json_api.test.scala` and paste this code into it:
      ```scala
      ${ScalaCliFiles.json_api_test.indent(6)}
      ```

      First run the API server in one shell:
      ```sh
      scala-cli test json_api.sc
      ```
      
      and then run the tests in another shell:
      ```sh
      scala-cli test json_api.test.scala
      ```
      """.md
    )
  )
}
