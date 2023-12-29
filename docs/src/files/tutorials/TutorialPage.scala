package files.tutorials

import utils.*
import Bundle.*

// TODO logging, logback + slf4j
// TODO docker

// TODO JWT
// TODO basic auth?


// TODO session?
// TODO cookie?

// https://undertow.io/javadoc/1.3.x/io/undertow/Handlers.html
// TODO websockets
// TODO SSE
trait TutorialPage extends DocPage {

  override def categoryPosts = List(
    Index,
    HelloWorld,
    PathParams,
    QueryParams,
    StaticFiles,
    HTML,
    HandlingForms,
    JsonAPI,
    Validation,
    SqlDb,
    Tests
  )

  override def pageCategory = Some("Tutorials")

  override def navbar = Some(Navbar.withActiveUrl(Index.ref))
}
