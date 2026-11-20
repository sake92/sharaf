//> using scala 3.9.0

import java.nio.file.{Files, Path}
import java.util.regex.Matcher
import scala.jdk.CollectionConverters.*

@main def syncExamples(args: String*): Unit =
  val write = args.toSeq match
    case Seq()          => false
    case Seq("--write") => true
    case _ =>
      System.err.println("Usage: scala docs/sync_examples.scala [-- --write]")
      sys.exit(2)

  val marker = "(?m)^<!-- example:(?:start .+|end) -->$".r
  val block = "(?ms)^<!-- example:start ([^\\r\\n]+) -->\\R.*?^<!-- example:end -->$".r

  def fail(message: String): Nothing =
    System.err.println(message)
    sys.exit(1)

  val docs = Files.walk(Path.of("docs", "content"))
  val pages =
    try docs.iterator().asScala.filter(path => Files.isRegularFile(path) && path.toString.endsWith(".md")).toList.sorted
    finally docs.close()

  val updates = pages.flatMap { page =>
    val original = Files.readString(page)
    val blocks = block.findAllMatchIn(original).toList
    if marker.findAllIn(original).size != blocks.size * 2 then
      fail(s"Unpaired example markers in $page")

    val updated = block.replaceAllIn(
      original,
      matched =>
        val source = Path.of(matched.group(1))
        if !Files.isRegularFile(source) then fail(s"Missing example in $page: $source")

        val code = Files.readString(source).stripLineEnd
        val longestRun = "`+".r.findAllIn(code).map(_.length).maxOption.getOrElse(0)
        val fence = "`" * math.max(3, longestRun + 1)
        Matcher.quoteReplacement(
          s"<!-- example:start $source -->\n${fence}scala\n$code\n$fence\n<!-- example:end -->"
        )
    )
    Option.when(updated != original)(page -> updated)
  }

  if updates.nonEmpty then
    if write then updates.foreach { (page, content) => Files.writeString(page, content) }
    val state = if write then "updated" else "out of date"
    System.err.println(s"Documentation snippets $state: ${updates.map(_._1).mkString(", ")}")
    if !write then sys.exit(1)
  else println("Documentation snippets match their examples")
