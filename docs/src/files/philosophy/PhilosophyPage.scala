package files.philosophy

trait PhilosophyPage extends utils.DocPage {

  override def categoryPosts = List(
    Index,
    Alternatives,
    RoutesMatching,
    QueryParamsHandling,
    DependencyInjection,
    Authentication
  )

  override def pageCategory = Some("Philosophy")

  override def currentCategoryPage = Some(Index)
}
