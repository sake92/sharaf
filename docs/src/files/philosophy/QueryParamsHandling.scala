package files.philosophy

import utils.Bundle.*

object QueryParamsHandling extends PhilosophyPage {

  override def pageSettings =
    super.pageSettings.withTitle("Routes Matching")

  override def blogSettings =
    super.blogSettings.withSections(firstSection, annotationsSection)

  val firstSection = Section(
    "Routes matching design",
    s"""


    WTF
    https://stackoverflow.com/questions/16942193/spring-mvc-complex-object-as-get-requestparam

    

    
    ---
    An 
    https://http4s.org/v0.23/docs/dsl.html#handling-query-parameters

    The ne

    """.md
  )

  val annotationsSection = Section(
    "Why not annotations?",
    s"""
    Let's see an example:
    ```scala
    @GetMapping(value = "/student/{studentId}")
    public Student getTestData(@PathVariable Integer studentId) {}

    @GetMapping(value = "/student/{studentId}")
    public Student getTestData2(@PathVariable Integer studentId) {}
    ```
    Issues:
    - the `studentId` appears in 2 places, you can make a typo and nothing will work.
    - the `"/student/{studentId}"` route is duplicated, there is no compiler support and it will fail only in runtime..
    """.md
  )

  val sharafSection = Section(
    "Sharaf's approach",
    s"""
    Sharaf does its route matching in plain scala code.  

    Scala's pattern matching warns you when you have duplicate routes, or "impossible" routes.  
    For example, if you handle 

    Let's see an example:
    ```scala
    @GetMapping(value = "/student/{studentId}")
    public Student getTestData(@PathVariable Integer studentId) {}

    @GetMapping(value = "/student/{studentId}")
    public Student getTestData2(@PathVariable Integer studentId) {}
    ```
    Issues:
    - the `studentId` appears in 2 places, you can make a typo and nothing will work.
    - the `"/student/{studentId}"` route is duplicated, there is no compiler support and it will fail only in runtime..
    """.md
  )

}
