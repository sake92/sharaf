---
title: Routing
description: Sharaf route and path reference
---

# {{ page.title }}

`Routes` wraps a partial function from `(HttpMethod, Path)` to `Response[?]`. A route can access the current request through `Request.current`.

| Form | Match |
| --- | --- |
| `GET -> Path()` | GET on `/` |
| `GET -> Path("cars")` | GET on `/cars` |
| `GET -> Path("cars", id)` | GET on `/cars/{id}`; `id` is a string |
| `GET -> Path("cars", param[Int](id))` | GET on `/cars/{id}` when `id` parses as an integer |
| `(GET \| POST) -> Path("cars")` | GET or POST on `/cars` |
| `GET -> Path("files", segments*)` | GET on `/files` and remaining path segments |

Methods are `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH`, and `HEAD`. `Path` is a sequence of string segments. A route that does not match falls through to the next case. `Routes.merge(Seq(first, second))` checks routes in that order.

The [routes how-to](/howtos/routes.html) covers enum, regex, and custom path parameters.
