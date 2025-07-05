---
title: Query Parameters
description: Sharaf How To Query Parameters
---

# {{ page.title }}


## How to bind query parameter as an enum?
  
Sharaf needs a `QueryStringRW[T]` instance for query params.  
It can automatically derive an instance for singleton enums:

```scala
enum Cloud derives QueryStringRW:
  case aws, gcp, azure

case class MyQueryParams(
  cloud: Cloud
) derives QueryStringRW
```
  
## How to bind optional query parameter?
  
The first option is to set the parameter to `Option[T]`:
```scala
case class MyQP(mandatory: String, opt: Option[Int]) derives QueryStringRW
```
If you make a request with params `?mandatory=abc`, `opt` will have value of `None`.

---
The second option is to set the parameter to some default value:
```scala
case class MyQP2(mandatory: String, opt: Int = 42) derives QueryStringRW
```
Here if you make a request with params `?mandatory=abc` the `opt` will have value of `42`.

> Note that you need the `-Yretain-trees` scalac flag turned on, otherwise it won't work!

## How to bind sequence query parameter?
  
Set the parameter to `Seq[T]`:
```scala
case class MyQP(seq: Seq[Int]) derives QueryStringRW
```

Let's consider a few possible requests with these query params:
- `?` (empty) -> `seq` will be empty `Seq()`
- `?seq=123` -> `seq` will be `Seq(123)`
- `?seq[]=123&seq[]=456` -> `seq` will be `Seq(123, 456)`
- `?seq[1]=123&seq[0]=456` -> `seq` will be `Seq(456, 123)` (note it is sorted here)

## How to bind composite query parameter?

You can make a common query params class and use it in multiple top-level query params, or standalone:
```scala
case class PageQP(page: Int, size: Int) derives QueryStringRW
case class MyQP(q: String, p: PageQP) derives QueryStringRW
```

Sharaf is quite lenient when parsing the query parameters, so all these combinations will work:
- `?q=abc&p.page=0&p.size=10` -> object style
- `?q=abc&p[page]=0&p[size]=10` -> brackets style
- `?q=abc&p[page]=0&p.size=10` -> mixed style (dont)


## How to bind a custom query parameter?

When you want to handle a custom *scalar* value in query params,
you need to implement a `QueryStringRW[T]` instance manually:
```scala
import ba.sake.querson.*

given QueryStringRW[MyType] with {
  override def write(path: String, value: MyType): QueryStringData =
    QueryStringRW[String].write(path, value.toString)

  override def parse(path: String, qsData: QueryStringData): MyType =
    val str = QueryStringRW[String].parse(path, qsData)
    Try(MyType.fromString(str)).toOption.getOrElse(typeError(path, "MyType", str))
}

private def typeError(path: String, tpe: String, value: Any): Nothing =
  throw ParsingException(ParseError(path, s"invalid \$tpe", Some(value)))
```

Then you can use it:
```scala
case class MyQueryParams(
  myType: MyType
) derives QueryStringRW
```

---
Note that Sharaf can automatically derive an instance for singleton enums.

