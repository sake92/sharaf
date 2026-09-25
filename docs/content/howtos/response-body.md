---
layout: howto.html
title: Response Body
description: Sharaf How To Response Body
---

# {{ page.title }}

Define a `ResponseWritable[T]` for your type. For example, to return XML:
```scala
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import ba.sake.sharaf.{HttpString, ResponseWritable}

case class MyXml(value: String)

given ResponseWritable[MyXml] with
  def write(value: MyXml, outputStream: OutputStream): Unit =
    outputStream.write(value.value.getBytes(StandardCharsets.UTF_8))

  def headers(value: MyXml): Seq[(HttpString, Seq[String])] =
    Seq(HttpString("Content-Type") -> Seq("application/xml; charset=utf-8"))
```

Then return it from a route:
```scala
Response.withBody(MyXml("<message>Hello</message>"))
```
