package utils

object Consts:

  val ProjectName = "Sharaf"

  val ArtifactOrg = "ba.sake"
  val ArtifactName = "sharaf"
  val ArtifactVersion = "0.10.0"

  val GhHandle = "sake92"
  val GhProjectName = "sharaf"
  val GhUrl = s"https://github.com/${GhHandle}/${GhProjectName}"
  val GhSourcesUrl = s"https://github.com/${GhHandle}/${GhProjectName}/tree/main"

  val tq = """""""""

  def allSearchIndexedPages = Seq(files.Index) ++
    files.tutorials.Index.categoryPosts ++
    files.howtos.Index.categoryPosts ++
    files.reference.Index.categoryPosts ++
    files.philosophy.Index.categoryPosts
