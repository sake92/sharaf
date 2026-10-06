---
layout: howto.html
title: Iron Query Parameters
description: Sharaf How To Iron Query Parameters
---

# {{ page.title }}

Add Iron to your project and import Querson's Iron instances:

```scala
import ba.sake.querson.QueryStringRW
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.ProblemDetails
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import sttp.model.StatusCode
```

Use a refined type in the query parameter case class:

```scala
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
```

Install a custom mapper before Sharaf's default JSON mapper:

```scala
val exceptionMapper: ExceptionMapper = {
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
}.orElse(ExceptionMapper.json)
```

Pass it to the server and read it with the normal request API:

```scala
val query = Request.current.queryParams[SearchQuery]
```

An invalid refined value produces a `422 Unprocessable Entity` validation response.
