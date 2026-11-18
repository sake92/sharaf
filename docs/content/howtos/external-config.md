---
layout: howto.html
title: External Config
description: Sharaf How To External Config
---

# {{ page.title }}

For JVM applications, including those packaged with GraalVM Native Image, add
[tupson-config](https://sake92.github.io/tupson/tutorials/parsing-config.html#usage-1) to parse
[Typesafe Config](https://github.com/lightbend/config) into Scala case classes:

```scala
//> using dep ba.sake::tupson-config:0.32.1
```

Use the equivalent dependency declaration in your build tool. Typesafe Config is included transitively;
`sharaf-undertow` already includes this dependency.

Then parse your configuration:

```scala
import java.net.URL
import com.typesafe.config.ConfigFactory
import ba.sake.tupson.{given, *}
import ba.sake.tupson.config.*

case class MyConf(
    port: Int,
    url: URL,
    string: String,
    seq: Seq[String]
) derives JsonRW

val rawConfig = ConfigFactory.parseString("""
    port = 7777
    url = "http://example.com"
    string = "str"
    seq = [a, "b", c]
""")

val myConf = rawConfig.parseConfig[MyConf]
// MyConf(7777,http://example.com,str,List(a, b, c))
```

