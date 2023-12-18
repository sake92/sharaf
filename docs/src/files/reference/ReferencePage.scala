package files.reference

import utils.*
import Bundle.*

trait ReferencePage extends DocPage {

  override def categoryPosts = List(Index)

  override def pageCategory = Some("Reference")

  override def navbar = Some(Navbar.withActiveUrl(Index.ref))
}
