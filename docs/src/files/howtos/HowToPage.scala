package files.howtos

import utils.*
import Bundle.*

// TODO CORS
// TODO custom path param matcher
// TODO custom query param matcher


trait HowToPage extends DocPage {

  override def categoryPosts =
    List(
      Index,
      EnumPathParam,
      RegexPathParam,
      OptionalQueryParam,
      SeqQueryParam,
      CompositeQueryParam,
      UploadFile,
      NotFound,
      ErrorHandler,
      ChainRoutes,
      ExternalConfig
    )

  override def pageCategory = Some("How-Tos")

  override def navbar = Some(Navbar.withActiveUrl(Index.ref))
}
