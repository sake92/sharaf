---
title: Request
description: Sharaf request API reference
---

# {{ page.title }}

Route handlers have a contextual `Request`; `Request.current` retrieves it.

| Member | Result or requirement |
| --- | --- |
| `headers` | `Map[HttpString, Seq[String]]` |
| `cookies` | `Seq[Cookie]` |
| `queryParamsRaw` | Raw query parameter map |
| `queryParams[T]` | Parsed value; requires `QueryStringRW[T]` |
| `queryParamsValidated[T]` | Parsed and validated value; also requires `Validator[T]` |
| `bodyString` | Request body as a string |
| `bodyJsonRaw` | JSON syntax tree (`JValue`) |
| `bodyJson[T]` | Parsed JSON; requires `JsonRW[T]` |
| `bodyJsonValidated[T]` | Parsed and validated JSON; also requires `Validator[T]` |
| `bodyFormRaw` | Raw form data map |
| `bodyForm[T]` | Parsed form data; requires `FormDataRW[T]` |
| `bodyFormValidated[T]` | Parsed and validated form data; also requires `Validator[T]` |

Query and form target types are products such as case classes or named tuples. Parsing and validation failures are mapped by the server's `ExceptionMapper`.

| Target type | Example |
| --- | --- |
| Case class | `case class Search(q: String) derives QueryStringRW` |
| Named tuple | `(q: String, perPage: Int)` |
| Union of named tuples | `(firstName: String) \| (lastName: String)` |
| Named tuple with a union field | `(id: Int \| String)` |

Named tuples do not currently support the validation methods. For validated input, use a case class with a `Validator` instance.
