---
title: Routes
description: Sharaf How To Routes
---

# {{ page.title }}

## How to match on multiple methods?

You can use the `|` operator in a pattern match:
```scala
case (GET | POST) -> Path() =>
...
```
You can always check the [Scala docs](https://docs.scala-lang.org/scala3/book/control-structures.html#handling-multiple-possible-matches-on-one-line)
for more help.

---
If you want to handle all possible methods, just don't use any extractors:
```scala
case method -> Path() =>
...
```
  

## How to match on multiple paths?
You can use the `|` operator in a pattern match:
```scala
case GET -> (Path("hello") | Path("hello-world")) =>
    ...
```
You can always check the [Scala docs](https://docs.scala-lang.org/scala3/book/control-structures.html#handling-multiple-possible-matches-on-one-line)
for more help.

---
If you want to handle all paths that start with "my-prefix/":
```scala
case GET -> Path("my-prefix", segments*) =>
    ...
```

---
If you want to handle all possible paths:
```scala
case GET -> Path(segments*) =>
    ...
```

## How to bind path parameter as an enum?
  
Sharaf needs a `FromPathParam[T]` instance for the `param[T]` extractor.  
It can automatically derive an instance for singleton enums:

```scala
enum Cloud derives FromPathParam:
  case aws, gcp, azure

val routes = Routes:
  case GET -> Path("pricing", param[Cloud](cloud)) =>
    Response.withBody(s"cloud = ${cloud}")
```

## How to bind path parameter as a regex?
  
```scala
val userIdRegex = "user_id_(\\d+)".r

val routes = Routes:
  case GET -> Path("pricing", userIdRegex(userId)) =>
    Response.withBody(s"userId = ${userId}")
```

Note that the `userId` is bound as a `String`.  

You could further match on it, for example:
```scala
val routes = Routes:
  case GET -> Path("pricing", userIdRegex(param[Int](userId))) =>
```
would extract `userId` as an `Int`.


## How to bind a custom path parameter?

Sharaf needs a `FromPathParam[T]` instance available:
```scala
import ba.sake.sharaf.routing.*

given FromPathParam[MyType] with {
  def parse(str: String): Option[MyType] =
    parseMyType(str) // impl here
}

val routes = Routes:
  case GET -> Path("pricing", param[MyType](myType)) =>
    Response.withBody(s"myType = ${myType}")
```

## How to split Routes?
  
When you have lots of routes, you will want to split them into multiple `Routes` handlers.  
Combining them is done with `Routes.merge`.  
The order of routes is preserved, of course:
```scala
val routes: Seq[Routes] = Seq(routes1, routes2, ... )

val allRoutes: Routes = Routes.merge(routes)
```

You can also `extend SharafController` instead of `Routes` directly.
```scala
class MyController1 extends SharafController:
  override def routes: Routes = Routes:
    case ...
class MyController2 extends SharafController:
  override def routes: Routes = Routes:
    case ...
    
val server = UndertowSharafServer(
  new MyController1, new MyController2
)
```
