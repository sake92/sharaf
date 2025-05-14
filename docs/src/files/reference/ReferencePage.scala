package files.reference

import utils.*

trait ReferencePage extends DocPage {

  override def categoryPosts = List(Index)

  override def pageCategory = Some("Reference")

}
