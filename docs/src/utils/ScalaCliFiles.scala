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

  val hello = get("hello.sc")
  val path_params = get("path_params.sc")
  val query_params = get("query_params.sc")
  val static_files = get("static_files.sc")
  val html_scalatags = get("html_scalatags.sc")
  val html_hepek = get("html_hepek.sc")
  val htmx_load_snippet = get(os.RelPath("htmx") / "htmx_load_snippet.sc")
  val form_handling = get("form_handling.sc")
  val json_api = get("json_api.sc")
  val json_api_test = get("json_api.test.scala")

  val sql_db = get("sql_db.sc")

  val validation = get("validation.sc")

  private def get(chunk: os.PathChunk) =
    // os.pwd is sandboxed, this is called from plugin !
    val wd = os.Path(System.getenv("MILL_WORKSPACE_ROOT"))
    os.read(wd / "examples" / "scala-cli" / chunk)
