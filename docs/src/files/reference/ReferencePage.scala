package files.reference

import files.tutorials.Index
import utils.*

trait ReferencePage extends DocPage {

  override def categoryPosts = List(Index)

  override def pageCategory = Some("Reference")

  override def currentCategoryPage = Some(Index)
}
