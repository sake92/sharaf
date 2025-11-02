
# Sharaf :nut_and_bolt:

Your new favorite, simple, intuitive, batteries-included scala3 web framework.

Documentation at https://sake92.github.io/sharaf/

Hello world example:
```scala
//> using scala 3.7.3
//> using dep ba.sake::sharaf-undertow:0.14.1

import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes {
  case GET -> Path("hello", name) =>
    Response.withBody(s"Hello $name")
}

UndertowSharafServer("localhost", 8181, routes).start()
```

## Cheatsheet

### Route Matching

```scala
// matches /hello/world
case GET -> Path("hello", "world") =>

// matches/hello/:name where name is a dynamic String variable
case GET -> Path("hello", name) =>

// matches/hello/:id where id is a dynamic Int variable
case GET -> Path("hello", param[Int](id)) =>

// matches a GET or POST request to /hello
case (GET | POST) -> Path("hello") =>

// matches any path that starts with /hello, e.g. /hello/a/b/c
case GET -> Path("hello", segments*) =>

// matches/hello/:cloud where cloud is a dynamic Cloud enum variable
enum Cloud derives FromPathParam:
  case aws, gcp, azure
...
case GET -> Path("hello", param[Cloud](cloud)) =>

// matches /hello/user_id_:userId where userId is a dynamic String variable
// here we use a Regex extractor
val userIdRegex = "user_id_(\\d+)".r
...
case GET -> Path("hello", userIdRegex(userId)) =>
```

### Handling Query Params

```scala
// raw map of query params: Map[String, Seq[String]]
val qp = Request.current.queryParamsRaw

// query params parsed into a case class
case class SearchParams(q: String, perPage: Int) derives QueryStringRW
val qp = Request.current.queryParams[SearchParams]

// query params parsed into a case class with validation
case class SearchParams(q: String, perPage: Int) derives QueryStringRW
object SearchParams {
  given Validator[SearchParams] = Validator.derived[SearchParams].notBlank(_.q)
}
val qp = Request.current.queryParamsValidated[SearchParams]

// query params parsed into a named tuple
val qp = Request.current.queryParams[(q: String, perPage: Int)]

// query params parsed into a named tuple with union type
val qp = Request.current.queryParams[(id: Int | String)]

// query params parsed into a union of named tuples
val qp = Request.current.queryParams[(firstName: String) | (lastName: String)]
```



### Handling Form Data

```scala
// raw map of form data: SeqMap[String, Seq[FormValue]]
val formData = Request.current.bodyFormRaw

// form data parsed into a case class
case class SearchParams(q: String, perPage: Int) derives FormDataRW
val formData = Request.current.bodyForm[SearchParams]

// form data parsed into a case class with validation
case class SearchParams(q: String, perPage: Int) derives FormDataRW
object SearchParams {
  given Validator[SearchParams] = Validator.derived[SearchParams].notBlank(_.q)
}
val formData = Request.current.bodyFormValidated[SearchParams]

// form data parsed into a named tuple
val formData = Request.current.bodyForm[(q: String, perPage: Int)]

// form data parsed into a named tuple with union type
val formData = Request.current.bodyForm[(id: Int | String)]

// form data parsed into a union of named tuples
val formData = Request.current.bodyForm[(firstName: String) | (lastName: String)]
```


### Handling JSON Data

```scala
// raw map of JSON data: JValue
val jsonData = Request.current.bodyJsonRaw

// JSON parsed into a case class
case class SearchParams(q: String, perPage: Int) derives JsonRW
val jsonData = Request.current.bodyJson[SearchParams]

// JSON parsed into a case class with validation
case class SearchParams(q: String, perPage: Int) derives JsonRW
object SearchParams {
  given Validator[SearchParams] = Validator.derived[SearchParams].notBlank(_.q)
}
val jsonData = Request.current.bodyJsonValidated[SearchParams]

// JSON parsed into a named tuple
val jsonData = Request.current.bodyJson[(q: String, perPage: Int)]

// JSON parsed into a named tuple with union type
val jsonData = Request.current.bodyJson[(id: Int | String)]

// JSON parsed into a union of named tuples
val jsonData = Request.current.bodyJson[(firstName: String) | (lastName: String)]
```

### Returning HTML

```scala
case GET -> Path() =>
  Response.withBody(IndexView)
...
// use safe html"" interpolator
// works very well in combo with HTMX
def IndexView =
  html"""
    <!DOCTYPE html>
    <html lang="en">
    <body>
    <div>
        <p>Welcome!</p>
        <a href="/hello/Bob">Hello world</a>
    </div>
    </body>
    </html>
  """
```

### Server Sent Events

```scala
case GET -> Path("sse-events") =>
  val sseSender = SseSender()
  new Thread(() => {
    for i <- 1 to 5 do
      sseSender.send(
        ServerSentEvent.Message(
          data = html"""<div>event${i}</div>""".toString
        )
      )
      Thread.sleep(1_000)
    sseSender.send(ServerSentEvent.Done())
  }).start()
  Response.withBody(sseSender)
```



