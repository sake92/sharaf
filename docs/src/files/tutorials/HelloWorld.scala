package files.tutorials

import utils.*
import Bundle.*, Tags.*

object HelloWorld extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("Hello World")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "Hello World",
    div(
      s"""
      Let's make a Hello World example in scala-cli.  
      Create a file `hello_sharaf.sc` and paste this code into it:
      ```scala
      ${ScalaCliFiles.hello.indent(6)}
      ```

      Then run it like this:
      ```sh
      scala-cli hello_sharaf.sc 
      ```
      Go to [http://localhost:8181/hello/Bob](http://localhost:8181/hello/Bob).  
      You will see a "Hello Bob" text response.

      ---
      The most interesting part is the `Routes` definition.  
      Here we pattern match on `(HttpMethod, Path)`.  
      The `Path` contains a `Seq[String]`, which are the parts of the URL you can match on.
      """.md
    )
  )
}
