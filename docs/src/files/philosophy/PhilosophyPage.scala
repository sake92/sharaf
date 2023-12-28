package files.philosophy

import utils.*
import Bundle.*

trait PhilosophyPage extends DocPage {

  override def categoryPosts = List(
    Index,
    RoutesMatching,
    DependencyInjection
  )

  override def pageCategory = Some("Philosophy")

  override def navbar = Some(Navbar.withActiveUrl(Index.ref))
}
