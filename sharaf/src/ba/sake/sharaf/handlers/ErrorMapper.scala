package ba.sake.sharaf.handlers

import java.net.URI
import scala.jdk.CollectionConverters.*
import org.typelevel.jawn.ast.*
import io.undertow.util.StatusCodes
import ba.sake.tupson
import ba.sake.tupson.{given, *}
import ba.sake.formson
import ba.sake.querson
import ba.sake.validson
import ba.sake.sharaf.*

/*
Why not HTTP content negotiation?
https://wiki.whatwg.org/wiki/Why_not_conneg
 */

type ErrorMapper = PartialFunction[Throwable, Response[?]]

object ErrorMapper {

  /*
  Only the exceptions **caused by sharaf internals** (e.g. parsing/validating request) are exposed.
  For example, if you parser JSON in your handler, that error WILL NOT BE EXPOSED/LEAKED to the user! :)
   */

  val default: ErrorMapper = {
    case e: NotFoundException =>
      Response.withBody(e.getMessage).withStatus(StatusCodes.NOT_FOUND)
    case se: SharafException =>
      Option(se.getCause()) match
        case Some(cause) =>
          cause match
            case e: validson.ValidationException =>
              val fieldValidationErrors = e.errors.mkString("[", "; ", "]")
              Response.withBody(s"Validation errors: $fieldValidationErrors").withStatus(StatusCodes.BAD_REQUEST)
            case e: querson.ParsingException =>
              Response.withBody(e.getMessage()).withStatus(StatusCodes.BAD_REQUEST)
            case e: tupson.ParsingException =>
              Response.withBody(e.getMessage()).withStatus(StatusCodes.BAD_REQUEST)
            case e: tupson.TupsonException =>
              Response.withBody(e.getMessage()).withStatus(StatusCodes.BAD_REQUEST)
            case e: formson.ParsingException =>
              Response.withBody(e.getMessage()).withStatus(StatusCodes.BAD_REQUEST)
            case other =>
              other.printStackTrace()
              Response.withBody("Server error").withStatus(StatusCodes.INTERNAL_SERVER_ERROR)
        case None =>
          se.printStackTrace()
          Response.withBody("Server error").withStatus(StatusCodes.INTERNAL_SERVER_ERROR)
  }

  val json: ErrorMapper = {
    case e: NotFoundException =>
      val problemDetails = ProblemDetails(StatusCodes.NOT_FOUND, "Not Found", e.getMessage)
      Response.withBody(problemDetails).withStatus(StatusCodes.NOT_FOUND)
    case se: SharafException =>
      Option(se.getCause()) match
        case Some(cause) =>
          cause match
            case e: validson.ValidationException =>
              val fieldValidationErrors =
                e.errors.map(err => ArgumentProblem(err.path, err.msg, Some(err.value.toString)))
              val problemDetails =
                ProblemDetails(StatusCodes.BAD_REQUEST, "Validation errors", invalidArguments = fieldValidationErrors)
              Response.withBody(problemDetails).withStatus(StatusCodes.BAD_REQUEST)
            case e: querson.ParsingException =>
              val parsingErrors = e.errors.map(err => ArgumentProblem(err.path, err.msg, err.value.map(_.toString)))
              val problemDetails =
                ProblemDetails(StatusCodes.BAD_REQUEST, "Invalid query parameters", invalidArguments = parsingErrors)
              Response.withBody(problemDetails).withStatus(StatusCodes.BAD_REQUEST)
            case e: tupson.ParsingException =>
              val parsingErrors = e.errors.map(err => ArgumentProblem(err.path, err.msg, err.value.map(_.toString)))
              val problemDetails =
                ProblemDetails(StatusCodes.BAD_REQUEST, "JSON Parsing errors", invalidArguments = parsingErrors)
              Response.withBody(problemDetails).withStatus(StatusCodes.BAD_REQUEST)
            case e: tupson.TupsonException =>
              Response
                .withBody(ProblemDetails(StatusCodes.BAD_REQUEST, "JSON parsing error", e.getMessage))
                .withStatus(StatusCodes.BAD_REQUEST)
            case e: formson.ParsingException =>
              Response
                .withBody(ProblemDetails(StatusCodes.BAD_REQUEST, "Form parsing error", e.getMessage))
                .withStatus(StatusCodes.BAD_REQUEST)
            case other =>
              other.printStackTrace()
              Response
                .withBody(ProblemDetails(StatusCodes.INTERNAL_SERVER_ERROR, "Server error", ""))
                .withStatus(StatusCodes.INTERNAL_SERVER_ERROR)
        case None =>
          se.printStackTrace()
          Response
            .withBody(ProblemDetails(StatusCodes.INTERNAL_SERVER_ERROR, "Server error", ""))
            .withStatus(StatusCodes.INTERNAL_SERVER_ERROR)
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
