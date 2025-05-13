package ba.sake.querson

import java.net.*
import scala.util.Try

given QueryStringRW[URL] with {
  override def write(path: String, value: URL): QueryStringData =
    QueryStringRW[String].write(path, value.toString)

  override def parse(path: String, qsData: QueryStringData): URL =
    val str = QueryStringRW[String].parse(path, qsData)
    Try(URI(str).toURL).toOption.getOrElse(typeError(path, "URL", str))
}

private def typeError(path: String, tpe: String, value: Any): Nothing =
  throw ParsingException(ParseError(path, s"invalid $tpe", Some(value)))
