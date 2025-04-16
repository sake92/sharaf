package files.philosophy

import utils.*
import Bundle.*

trait PhilosophyPage extends DocPage {

  override def categoryPosts = List(
    Index,
    Alternatives,
    RoutesMatching,
    QueryParamsHandling,
    DependencyInjection,
    Authentication
  )

  override def pageCategory = Some("Philosophy")

  override def navbar = Some(Navbar.withActiveUrl(Index.ref))
}
