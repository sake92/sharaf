---
title: Errors
description: Sharaf exception mapping reference
---

# {{ page.title }}

`ExceptionMapper` is a `PartialFunction[Throwable, Response[?]]`. Server constructors use `ExceptionMapper.default` unless another mapper is provided. `ExceptionMapper.json` returns JSON problem details.

| Condition | HTTP status |
| --- | --- |
| `NotFoundException` | 404 |
| `RejectedException` | 403 |
| `MethodNotAllowedException` | 405 |
| Query, JSON, or form parsing failure | 400 |
| Validation failure | 422 |
| Other nonfatal exception | 500 |

The JSON mapper's problem details include `status`, `title`, `detail`, `type`, `instance`, and `invalidArguments`. Unhandled errors use a generic server error response.

See [custom exception handlers](/howtos/exception-handler.html).
