package files.howtos

import utils.Bundle.*

object ResponseBody extends HowToPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Custom Response Body")
    .withLabel("Custom Response Body")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How to use a custom response body?",
    s"""
    You need to define a custom `ResponseWritable[T]` for your type `T`.
    
    Let's say you have a `MyXML` class, and you want to use it as a response body.  
    You would write something like this:
    ```scala
    given ResponseWritable[MyXML] with {
      override def write(value: MyXML, exchange: HttpServerExchange): Unit =
        exchange.getResponseSender.send(value.asString)
      override def headers(value: String): Seq[(HttpString, Seq[String])] = Seq(
        Headers.CONTENT_TYPE -> Seq("text/xml")
      )
    }
    ```
    
    Now you can use `MyXML` as a response body:
    ```scala
    val myXml = MyXML(...)
    Response.withBody(myXml)
    ```
    
    """.md
  )
}
