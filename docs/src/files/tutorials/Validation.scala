package files.tutorials

import utils.*
import Bundle.*

object Validation extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("Validation")

  override def blogSettings =
    super.blogSettings.withSections(helloSection)

  val helloSection = Section(
    "Validating data",
    s"""
    For validating data you need to use the `Validator` typeclass.  
    A small example:

    ```scala
    import ba.sake.validson.Validator

    case class ValidatedData(num: Int, str: String, seq: Seq[String])

    object ValidatedData:
      given Validator[ValidatedData] = Validator
        .derived[ValidatedData]
        .and(_.num, _ > 0, "must be positive")
        .and(_.str, !_.isBlank, "must not be blank")
        .and(_.seq, _.nonEmpty, "must not be empty")
        .and(_.seq, _.forall(_.size == 2), "must have elements of size 2")
    ```

    The `ValidatedData` can be any `case class`: json data, form data, query params..  

    ---
  
    Let's see a full-blown example with validation:

    ```scala
    //> using scala "3.3.1"
    //> using dep ba.sake::sharaf:0.0.17

    import io.undertow.Undertow
    import ba.sake.querson.QueryStringRW
    import ba.sake.tupson.JsonRW
    import ba.sake.validson.Validator
    import ba.sake.sharaf.*, routing.*

    case class Car(brand: String, model: String, quantity: Int) derives JsonRW
    object Car:
      given Validator[Car] = Validator
        .derived[Car]
        .and(_.brand, !_.isBlank, "must not be blank")
        .and(_.model, !_.isBlank, "must not be blank")
        .and(_.quantity, _ >= 0, "must not be negative")

    case class CarQuery(brand: String) derives QueryStringRW
    object CarQuery:
      given Validator[CarQuery] = Validator
        .derived[CarQuery]
        .and(_.brand, !_.isBlank, "must not be blank")

    case class CarApiResult(message: String) derives JsonRW

    val routes = Routes:
      case GET() -> Path("cars") =>
        val qp = Request.current.queryParamsValidated[CarQuery]
        Response.withBody(CarApiResult("Query OK"))

      case POST() -> Path("cars") =>
        val qp = Request.current.bodyJsonValidated[Car]
        Response.withBody(CarApiResult("JSON body OK"))

    Undertow.builder
      .addHttpListener(8181, "localhost")
      .setHandler(
        SharafHandler(routes).withErrorMapper(ErrorMapper.json)
      )
      .build
      .start()

    println(s"Server started at http://localhost:8181")
    ```

    Notice above that we used `queryParamsValidated` and not plain `queryParams` (does not validate query params).  
    Also, for JSON body parsing+validation we use `bodyJsonValidated` and not plain `bodyJson` (does not validate JSON body).  

    ---
    When you do a GET [http://localhost:8181/cars?brand=  ](http://localhost:8181/cars?brand=  )  
    you will get a nice JSON error message with HTTP Status of `400 Bad Request`:
    ```json
    {
      "instance": null,
      "invalidArguments": [
        {
          "reason": "must not be blank",
          "path": "$$.brand",
          "value": ""
        }
      ],
      "detail": "",
      "type": null,
      "title": "Validation errors",
      "status": 400
    }
    ```

    The error message format follows the [RFC 7807 problem detail](https://datatracker.ietf.org/doc/html/rfc7807).

    ---

    When you do a POST [http://localhost:8181/cars](http://localhost:8181/cars) with a malformed body:
    ```json
    {
      "brand": " ",
      "model": "ML350",
      "quantity": -5
    }
    ```

    you will get these errors:
    ```json
    {
      "instance": null,
      "invalidArguments": [
        {
          "reason": "must not be blank",
          "path": "$$.brand",
          "value": " "
        },
        {
          "reason": "must not be negative",
          "path": "$$.quantity",
          "value": "-5"
        }
      ],
      "detail": "",
      "type": null,
      "title": "Validation errors",
      "status": 400
    }
    ```
    """.md
  )

}
