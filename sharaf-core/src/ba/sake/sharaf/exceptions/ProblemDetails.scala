package ba.sake.sharaf.exceptions

import ba.sake.tupson.{*, given}

import java.net.URI

// https://www.rfc-editor.org/rfc/rfc7807#section-3.1
case class ProblemDetails(
    status: Int, // http status code
    title: String, // short summary
    detail: String = "",
    `type`: Option[URI] = None, // general error description URL
    instance: Option[URI] = None, // this particular error URL
    invalidArguments: Seq[ProblemDetails.ArgumentProblem] = Seq.empty
) derives JsonRW

object ProblemDetails:

  case class ArgumentProblem(
      path: String,
      reason: String,
      value: Option[String]
  ) derives JsonRW
