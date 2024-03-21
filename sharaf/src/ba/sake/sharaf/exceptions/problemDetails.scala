package ba.sake.sharaf.exceptions


import java.net.URI
import ba.sake.tupson.{*, given}

// https://www.rfc-editor.org/rfc/rfc7807#section-3.1
case class ProblemDetails(
    status: Int, // http status code
    title: String, // short summary
    detail: String = "",
    `type`: Option[URI] = None, // general error description URL
    instance: Option[URI] = None, // this particular error URL
    invalidArguments: Seq[ArgumentProblem] = Seq.empty
) derives JsonRW

case class ArgumentProblem(
    path: String,
    reason: String,
    value: Option[String]
) derives JsonRW

