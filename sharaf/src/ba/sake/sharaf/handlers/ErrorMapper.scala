package ba.sake.sharaf.handlers

import scala.jdk.CollectionConverters.*

import ba.sake.tupson
import ba.sake.tupson.JsonRW
import ba.sake.formson
import ba.sake.querson
import ba.sake.sharaf.*
import java.net.URI
import org.typelevel.jawn.ast.*
import ba.sake.validson.ValidationException

/*
Why not HTTP content negotiation?
https://wiki.whatwg.org/wiki/Why_not_conneg
 */

type ErrorMapper = PartialFunction[Throwable, Response[?]]

object ErrorMapper {

  val default: ErrorMapper = {
    case e: NotFoundException =>
      Response.withBody(e.getMessage).withStatus(404)
    case e: ValidationException =>
      val fieldValidationErrors = e.errors.mkString("[", "; ", "]")
      Response.withBody(s"Validation errors: $fieldValidationErrors").withStatus(400)
    // query
    case e: querson.ParsingException =>
      Response.withBody(e.getMessage()).withStatus(400)
    // json
    case e: tupson.ParsingException =>
      Response.withBody(e.getMessage()).withStatus(400)
    case e: tupson.TupsonException =>
      Response.withBody(e.getMessage()).withStatus(400)
    // form
    case e: formson.ParsingException =>
      Response.withBody(e.getMessage()).withStatus(400)
  }

  val json: ErrorMapper = {
    case e: NotFoundException =>
      val problemDetails = ProblemDetails(404, "Not Found", e.getMessage)
      Response.withBody(problemDetails).withStatus(404)
    case e: ValidationException =>
      val fieldValidationErrors = e.errors.map(err => ArgumentProblem(err.path, err.msg, Some(err.value.toString)))
      val problemDetails = ProblemDetails(400, "Validation errors", invalidArguments = fieldValidationErrors)
      Response.withBody(problemDetails).withStatus(400)
    // query
    case e: querson.ParsingException =>
      val parsingErrors = e.errors.map(err => ArgumentProblem(err.path, err.msg, err.value.map(_.toString)))
      val problemDetails = ProblemDetails(400, "Invalid query parameters", invalidArguments = parsingErrors)
      Response.withBody(problemDetails).withStatus(400)
    // json
    case e: tupson.ParsingException =>
      val parsingErrors = e.errors.map(err => ArgumentProblem(err.path, err.msg, err.value.map(_.toString)))
      val problemDetails = ProblemDetails(400, "JSON Parsing errors", invalidArguments = parsingErrors)
      Response.withBody(problemDetails).withStatus(400)
    case e: tupson.TupsonException =>
      Response.withBody(ProblemDetails(400, "JSON parsing error", e.getMessage)).withStatus(400)
    // form
    case e: formson.ParsingException =>
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
