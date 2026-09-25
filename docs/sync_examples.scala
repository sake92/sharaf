//> using scala 3.7.3

import java.nio.file.{Files, Path, StandardCopyOption}
import java.util.Arrays

@main def syncExamples(args: String*): Unit =
  val write = args.toSeq match
    case Seq()          => false
    case Seq("--write") => true
    case _ =>
      System.err.println("Usage: scala docs/sync_examples.scala [-- --write]")
      sys.exit(2)

  val examples = Seq(
    "form_handling.sc" -> "examples/scala-cli/form_handling.sc",
    "hello.sc" -> "examples/scala-cli/hello.sc",
    "html.sc" -> "examples/scala-cli/html.sc",
    "htmx_load_snippet.sc" -> "examples/htmx/htmx_load_snippet.sc",
    "json_api.test.scala" -> "examples/scala-cli/json_api.test.scala",
    "path_params.sc" -> "examples/scala-cli/path_params.sc",
    "query_params.sc" -> "examples/scala-cli/query_params.sc",
    "sql_db.sc" -> "examples/scala-cli/sql_db.sc",
    "static_files.sc" -> "examples/scala-cli/static_files.sc",
    "validation.sc" -> "examples/scala-cli/validation.sc"
  )

  val drift = examples.flatMap { (name, sourceName) =>
    val source = Path.of(sourceName)
    val target = Path.of("docs", "_includes", name)
    if !Files.isRegularFile(source) then
      System.err.println(s"Missing example: $source")
      sys.exit(1)

    if !Files.isRegularFile(target) || !Arrays.equals(Files.readAllBytes(source), Files.readAllBytes(target)) then
      if write then Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
      Some(name)
    else None
  }

  if drift.nonEmpty then
    val state = if write then "updated" else "out of date"
    System.err.println(s"Documentation snippets $state: ${drift.mkString(", ")}")
    if !write then sys.exit(1)
  else println("Documentation snippets match their examples")
