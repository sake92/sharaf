package utils

// TODO extract to mill-hepek somehow
extension (str: String) {

  /** @param from
    *   Inclusive
    * @param until
    *   Exclusive
    * @return
    */
  def snippet(from: String = "", until: String = ""): String =
    str.linesWithSeparators
      .dropWhile(line => from != "" && !line.trim.startsWith(from))
      .takeWhile(line => until == "" || !line.trim.startsWith(until))
      .mkString
}

object ScalaCliFiles:

  val hello = get(os.RelPath("scala-cli/hello.sc"))
  val path_params = get(os.RelPath( "scala-cli/path_params.sc"))
  val query_params = get(os.RelPath("scala-cli/query_params.sc"))
  val static_files = get(os.RelPath("scala-cli/static_files.sc"))
  val html_scalatags = get(os.RelPath("scala-cli/html_scalatags.sc"))
  val html_hepek = get(os.RelPath("scala-cli/html_hepek.sc"))
  val htmx_load_snippet = get(os.RelPath("htmx/htmx_load_snippet.sc"))
  val form_handling = get(os.RelPath("scala-cli/form_handling.sc"))
  val json_api = get(os.RelPath("scala-cli/json_api.sc"))
  val json_api_test = get(os.RelPath("scala-cli/json_api.test.scala"))

  val sql_db = get(os.RelPath("scala-cli/sql_db.sc"))

  val validation = get(os.RelPath("scala-cli/validation.sc"))

  private def get(chunk: os.PathChunk) =
    // os.pwd is sandboxed, this is called from plugin !
    val wd = os.Path(System.getenv("MILL_WORKSPACE_ROOT"))
    os.read(wd / "examples" / chunk)
