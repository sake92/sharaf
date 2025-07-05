---
title: Response Body
description: Sharaf How To Response Body
---

# {{ page.title }}

How to use a custom response body?

You need to define a custom `ResponseWritable[T]` for your type `T`.

Let's say you have a `MyXML` class, and you want to use it as a response body.  
You would write something like this:
```scala
given ResponseWritable[MyXML] with {
    override def write(value: MyXML, exchange: HttpServerExchange): Unit =
      exchange.getResponseSender.send(value.asString)
    override def headers(value: String): Seq[(HttpString, Seq[String])] = Seq(
      HttpString(HeaderNames.ContentType) -> Seq("text/xml")
    )
}
```

Now you can use `MyXML` as a response body:
```scala
val myXml = MyXML(...)
Response.withBody(myXml)
```
