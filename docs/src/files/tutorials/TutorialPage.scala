package files.tutorials

import utils.*
import Bundle.*

trait TutorialPage extends DocPage {

  override def categoryPosts = List(
    Index,
    HelloWorld,
    QueryParams,
    HTML,
    StaticFiles,
    HandlingForms,
    JsonAPI,
    Validation
  )

  override def pageCategory = Some("Tutorials")

  override def navbar = Some(Navbar.withActiveUrl(Index.ref))
}
