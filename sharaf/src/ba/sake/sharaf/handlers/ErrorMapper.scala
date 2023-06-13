package ba.sake.sharaf.handlers

import scala.jdk.CollectionConverters.*

import ba.sake.tupson.*
import ba.sake.formson.*
import ba.sake.sharaf.*
import java.net.URI
import org.typelevel.jawn.ast.*
import ba.sake.validation.FieldsValidationException

///////////// TODO Seq[ErrorMapper] with content types..
type ErrorMapper[T] = PartialFunction[Throwable, Response[T]]

object ErrorMapper {
  val empty: ErrorMapper[String] = new PartialFunction[Throwable, Response[String]] {

    override def apply(v1: Throwable): Response[String] = ???

    override def isDefinedAt(x: Throwable): Boolean = false
  }

  val default: ErrorMapper[String] = {
    case e: NotFoundException =>
      Response.withBody(e.getMessage).withStatus(404)
    case e: FieldsValidationException =>
      val fieldValidationErrors = e.errors.mkString("[", "; ", "]")
      Response.withBody(s"Validation errors: $fieldValidationErrors").withStatus(400)
    // json
    case e: ParsingException =>
      Response.withBody(e.getMessage()).withStatus(400)
    case e: TupsonException =>
      Response.withBody(e.getMessage()).withStatus(400)
    // form
    case e: FormsonException =>
      Response.withBody(e.getMessage()).withStatus(400)
  }

  val json: ErrorMapper[ProblemDetails] = {
    case e: NotFoundException =>
      val problemDetails = ProblemDetails(404, "Not Found", e.getMessage)
      Response.withBody(problemDetails).withStatus(404)
    case e: FieldsValidationException =>
      val fieldValidationErrors = e.errors.map(err => ArgumentProblem(err.path, err.msg, Some(err.value.toString)))
      val problemDetails = ProblemDetails(400, "Validation errors", invalidArguments = fieldValidationErrors)
      Response.withBody(problemDetails).withStatus(400)
    // json
    case e: ParsingException =>
      val parsingErrors = e.errors.map(err => ArgumentProblem(err.path, err.msg, err.value.map(_.toString)))
      val problemDetails = ProblemDetails(400, "JSON Parsing errors", invalidArguments = parsingErrors)
      Response.withBody(problemDetails).withStatus(400)
    case e: TupsonException =>
      Response.withBody(ProblemDetails(400, "JSON parsing error", e.getMessage)).withStatus(400)
    // form
    case e: FormsonException =>
      Response.withBody(ProblemDetails(400, "Form parsing error", e.getMessage)).withStatus(400)
  }

}

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
