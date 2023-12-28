package files.tutorials

import utils.*
import Bundle.*

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
