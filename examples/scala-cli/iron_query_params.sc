//> using dep ba.sake::sharaf-undertow:0.20.0
//> using dep io.github.iltotore::iron:3.3.2

import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.{ExceptionMapper, ProblemDetails}
import ba.sake.sharaf.undertow.UndertowSharafServer
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import sttp.model.StatusCode

final case class IronException(errors: Seq[ProblemDetails.ArgumentProblem]) extends Exception

given [A, C](using rw: QueryStringRW[A], constraint: RuntimeConstraint[A, C]): QueryStringRW[A :| C] with {
  override def write(path: String, value: A :| C): QueryStringData = rw.write(path, value)

  override def parse(path: String, data: QueryStringData): A :| C = {
    val value = rw.parse(path, data)
    value.refineEither[C].fold(
      message => throw IronException(Seq(ProblemDetails.ArgumentProblem(path, message, Some(value.toString)))),
      identity
    )
  }
}

type SearchTerm = String :| Not[Blank]
case class SearchQuery(term: SearchTerm) derives QueryStringRW

val ironMapper: ExceptionMapper = {
  case error: IronException =>
    Response
      .withBody(
        ProblemDetails(
          StatusCode.UnprocessableEntity.code,
          "Validation errors",
          invalidArguments = error.errors
        )
      )
      .withStatus(StatusCode.UnprocessableEntity)
}
val exceptionMapper: ExceptionMapper = ironMapper.orElse(ExceptionMapper.json)

val routes = Routes {
  case GET -> Path("search") =>
    Response.withBody(Request.current.queryParams[SearchQuery].term)
}

UndertowSharafServer("localhost", 8181, routes, exceptionMapper = exceptionMapper).start()
