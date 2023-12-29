package files.howtos

import utils.*
import Bundle.*

// TODO CORS

trait HowToPage extends DocPage {

  override def categoryPosts =
    List(
      Index,
      MatchMultipleMethods,
      MatchMultiplePaths,
      EnumPathParam,
      RegexPathParam,
      CustomPathParam,
      EnumQueryParam,
      OptionalQueryParam,
      SeqQueryParam,
      CompositeQueryParam,
      CustomQueryParam,
      UploadFile,
      NotFound,
      ErrorHandler,
      ChainRoutes,
      ExternalConfig
    )

  override def pageCategory = Some("How-Tos")

  override def navbar = Some(Navbar.withActiveUrl(Index.ref))
}
