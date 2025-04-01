package files.howtos

import utils.*
import Bundle.*

trait HowToPage extends DocPage {

  override def categoryPosts =
    List(
      Index,
      Redirect,
      Routes,
      QueryParams,
      ResponseBody,
      UploadFile,
      NotFound,
      ExceptionHandler,
      ExternalConfig,
      CORS
    )

  override def pageCategory = Some("How-Tos")

  override def navbar = Some(Navbar.withActiveUrl(Index.ref))
}
