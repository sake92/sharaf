package files.philosophy

import utils.Bundle.*

object Alternatives extends PhilosophyPage {

  override def pageSettings =
    super.pageSettings.withTitle("Alternatives")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "What about other frameworks?",
    s"""

    ### Async frameworks like Play, Akka HTTP etc
    Synchronous programming is much, much easier to understand, debug, profile etc..  
    Benefits (performance/throughput) of async handling are mostly void in Java 21, with introduction of Virtual threads. Yay! 

    Only bummer for now is that Undertow doesn't still support them.. :/  
    But undertow is performant in the current shape too, so for most use cases it will be enough.

    ### Pure FP libs like http4s, zio-http etc

    Too much focus on purely functional programming and (mostly unnecessary) math concepts.  
    Easy to get lost in that and overcomplicate your code.

    ### Enterprise frameworks like Spring Framework, Quarkus etc
    Too much annotations, autoconfigurations, dependency injection and complexity.

    ### Standalone JEE servers like Tomcat, Jetty etc
    I was looking into these, but then sharaf would have to depend on Servlets API,  
    use `@Inject` and gazzilion of god-knows-what-they-do annotations just to configure OAuth2 for example...


    """.md
  )

}
