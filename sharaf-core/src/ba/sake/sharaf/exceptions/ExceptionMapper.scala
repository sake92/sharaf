package ba.sake.sharaf.exceptions

import sttp.model.StatusCode
import ba.sake.tupson
import ba.sake.formson
import ba.sake.querson
import ba.sake.validson
import ba.sake.sharaf.*
import ProblemDetails.ArgumentProblem

/*
Why not HTTP content negotiation?
https://wiki.whatwg.org/wiki/Why_not_conneg
 */

type ExceptionMapper = PartialFunction[Throwable, Response[?]]

object ExceptionMapper {

  /*
  Only the exceptions **caused by sharaf internals** (e.g. parsing/validating request) are exposed.
  For example, if you parser JSON in your handler, that error WILL NOT BE EXPOSED/LEAKED to the user! :)
   */

  val default: ExceptionMapper = {
    case e: NotFoundException =>
      Response.withBody(e.getMessage).withStatus(StatusCode.NotFound)
    case e: RejectedException =>
      Response.withBody(e.getMessage).withStatus(StatusCode.Forbidden)
    case e: MethodNotAllowedException =>
      Response.withBody(e.getMessage).withStatus(StatusCode.MethodNotAllowed)
    case se: SharafException =>
      Option(se.getCause) match
        case Some(cause) =>
          cause match
            case e: validson.ValidsonException =>
              val fieldValidationErrors = e.errors.mkString("[", "; ", "]")
              Response
                .withBody(s"Validation errors: $fieldValidationErrors")
                .withStatus(StatusCode.UnprocessableEntity)
            case e: querson.ParsingException =>
              Response.withBody(e.getMessage).withStatus(StatusCode.BadRequest)
            case e: tupson.ParsingException =>
              Response.withBody(e.getMessage).withStatus(StatusCode.BadRequest)
            case e: tupson.TupsonException =>
              Response.withBody(e.getMessage).withStatus(StatusCode.BadRequest)
            case e: formson.ParsingException =>
              Response.withBody(e.getMessage).withStatus(StatusCode.BadRequest)
            case other =>
              other.printStackTrace()
              Response.withBody("Server error").withStatus(StatusCode.InternalServerError)
        case None =>
          se.printStackTrace()
          Response.withBody("Server error").withStatus(StatusCode.InternalServerError)
  }

  val json: ExceptionMapper = {
    case e: NotFoundException =>
      val problemDetails = ProblemDetails(StatusCode.NotFound.code, "Not Found", e.getMessage)
      Response.withBody(problemDetails).withStatus(StatusCode.NotFound)
    case se: SharafException =>
      Option(se.getCause) match
        case Some(cause) =>
          cause match
            case e: validson.ValidsonException =>
              val fieldValidationErrors =
                e.errors.map(err => ArgumentProblem(err.path, err.msg, Some(err.value.toString)))
              val problemDetails =
                ProblemDetails(
                  StatusCode.UnprocessableEntity.code,
                  "Validation errors",
                  invalidArguments = fieldValidationErrors
                )
              Response.withBody(problemDetails).withStatus(StatusCode.UnprocessableEntity)
            case e: querson.ParsingException =>
              val parsingErrors = e.errors.map(err => ArgumentProblem(err.path, err.msg, err.value.map(_.toString)))
              val problemDetails =
                ProblemDetails(StatusCode.BadRequest.code, "Invalid query parameters", invalidArguments = parsingErrors)
              Response.withBody(problemDetails).withStatus(StatusCode.BadRequest)
            case e: tupson.ParsingException =>
              val parsingErrors = e.errors.map(err => ArgumentProblem(err.path, err.msg, err.value.map(_.toString)))
              val problemDetails =
                ProblemDetails(StatusCode.BadRequest.code, "JSON Parsing errors", invalidArguments = parsingErrors)
              Response.withBody(problemDetails).withStatus(StatusCode.BadRequest)
            case e: tupson.TupsonException =>
              Response
                .withBody(ProblemDetails(StatusCode.BadRequest.code, "JSON parsing error", e.getMessage))
                .withStatus(StatusCode.BadRequest)
            case e: formson.ParsingException =>
              Response
                .withBody(ProblemDetails(StatusCode.BadRequest.code, "Form parsing error", e.getMessage))
                .withStatus(StatusCode.BadRequest)
            case other =>
              other.printStackTrace()
              Response
                .withBody(ProblemDetails(StatusCode.InternalServerError.code, "Server error", ""))
                .withStatus(StatusCode.InternalServerError)
        case None =>
          se.printStackTrace()
          Response
            .withBody(ProblemDetails(StatusCode.InternalServerError.code, "Server error", ""))
            .withStatus(StatusCode.InternalServerError)
  }

}
