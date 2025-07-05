---
title: External Config
description: Sharaf How To External Config
---

# {{ page.title }}

The [typesafe config](https://github.com/lightbend/config) library is already included in Sharaf.  
Also included is the [tupson-config](https://sake92.github.io/tupson/tutorials/parsing-config.html#usage-1) which simplifies the process:

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

