package files.howtos

import utils.*

trait HowToPage extends utils.DocPage {

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

  override def currentCategoryPage = Some(Index)
}
