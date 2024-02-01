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
  val html = get("html.sc")
  val htmx_load_snippet = get(os.RelPath("htmx") / "htmx_load_snippet.sc")
  val form_handling = get("form_handling.sc")
  val json_api = get("json_api.sc")
  val json_api_test = get("json_api.test.scala")

  val sql_db = get("sql_db.sc")

  val validation = get("validation.sc")

  private def get(chunk: os.PathChunk) =
    os.read(os.pwd / "examples" / "scala-cli" / chunk)
