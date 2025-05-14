package files.tutorials

import utils.*

object HandlingForms extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("Handling Forms")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "Handling Form data",
    s"""
    Form data can be extracted with `Request.current.bodyForm[MyData]`.  
    The `MyData` needs to have a `FormDataRW` given instance.

    Create a file `form_handling.sc` and paste this code into it:
    ```scala
    ${ScalaCliFiles.form_handling.indent(4)}
    ```

    Then run it like this:
    ```sh
    scala-cli form_handling.sc 
    ```

    Now go to [http://localhost:8181](http://localhost:8181)
    and fill in the page with some data.

    When you click the "Submit" button you will see a response like this:
    ```
    Got form data: ContactUsForm(Bob,bob@example.com)
    ```
    """.md
  )
}
