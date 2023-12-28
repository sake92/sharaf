package files.howtos

import utils.*
import Bundle.*

trait HowToPage extends DocPage {

  override def categoryPosts =
    List(Index, EnumPathParam, RegexPathParam, OptionalQueryParam, SeqQueryParam, CompositeQueryParam, UploadFile)

  override def pageCategory = Some("How-Tos")

  override def navbar = Some(Navbar.withActiveUrl(Index.ref))
}
