package files.tutorials

import utils.*


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
        .positive(_.num)
        .notBlank(_.str)
        .minItems(_.seq, 1)
    ```

    The `ValidatedData` can be any `case class`: json data, form data, query params..  

    ---
  
    Create a file `validation.sc` and paste this code into it:

    ```scala
    ${ScalaCliFiles.validation.indent(4)}
    ```

    Then run it like this:
    ```sh
    scala-cli validation.sc 
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
          "path": "\\$$.brand",
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
          "path": "\\$$.brand",
          "value": " "
        },
        {
          "reason": "must not be negative",
          "path": "\\$$.quantity",
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
