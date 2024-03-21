package files.howtos

import utils.*
import Bundle.*

// TODO custom response body

trait HowToPage extends DocPage {

  override def categoryPosts =
    List(
      Index,
      Redirect,
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
      ExceptionHandler,
      SplitRoutes,
      ExternalConfig,
      CORS
    )

  override def pageCategory = Some("How-Tos")

  override def navbar = Some(Navbar.withActiveUrl(Index.ref))
}
