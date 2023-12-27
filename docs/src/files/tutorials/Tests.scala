package files.tutorials

import utils.*
import Bundle.*, Tags.*

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

      Here we are testing the API from the [JSON API tutorial](${JsonAPI.routesSection.ref}):
      ```scala
      //> using scala "3.3.1"
      //> using dep ba.sake::sharaf:0.0.18
      //> using test.dep org.scalameta::munit::0.7.29

      import io.undertow.Undertow
      import ba.sake.tupson.*

      case class Car(brand: String, model: String, quantity: Int) derives JsonRW

      class JsonApiSuite extends munit.FunSuite {

        val baseUrl = "http://localhost:8181"

        test("create and get cars") {
          locally {
            val res = requests.get(s"$$baseUrl/cars")
            val resBody = res.text.parseJson[Seq[Car]]
            assertEquals(res.statusCode, 200)
            assertEquals(res.headers("content-type"), Seq("application/json"))
            assertEquals(res.text.parseJson[Seq[Car]], Seq.empty)
          }

          locally {
            val body = Car("Mercedes", "ML350", 1)
            val res = requests.post(s"$$baseUrl/cars", data = body.toJson)
            assertEquals(res.statusCode, 200)
          }

          locally {
            val res = requests.get(s"$$baseUrl/cars/Mercedes")
            val resBody = res.text.parseJson[Seq[Car]]
            assertEquals(res.statusCode, 200)
            assertEquals(res.headers("content-type"), Seq("application/json"))
            assertEquals(resBody, Seq(Car("Mercedes", "ML350", 1)))
          }
        }
      }
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
