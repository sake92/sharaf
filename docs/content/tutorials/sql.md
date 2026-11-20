---
layout: tutorial.html
title: SQL
description: Store and read data with Sharaf and Squery
---

# {{ page.title }}

In this lesson, you will add a customer and read it back. The example uses an in-memory H2 database, so no database setup is needed.

Create a file named `sql_db.sc`:

<!-- example:start examples/scala-cli/sql_db.sc -->
```scala
//> using scala 3.9.0
//> using dep com.h2database:h2:2.4.240
//> using dep ba.sake::sharaf-potateh:0.24.0
//> using dep ba.sake::squery:0.8.2

import ba.sake.tupson.JsonRW
import ba.sake.squery.{*, given}
import ba.sake.sharaf.*
import ba.sake.sharaf.potateh.PotatehSharafServer

val ds = org.h2.jdbcx.JdbcDataSource()
ds.setUrl("jdbc:h2:mem:mydb;DB_CLOSE_DELAY=-1")

val ctx = new SqueryContext(ds)
ctx.runTransaction {
  sql"""
    CREATE TABLE customers(
      id SERIAL PRIMARY KEY,
      name VARCHAR
    )
  """.update()
}

case class Customer(name: String) derives JsonRW

val routes = Routes:
  case GET -> Path("customers") =>
    val customerNames = ctx.run {
      sql"SELECT name FROM customers".readValues[String]()
    }
    Response.withBody(customerNames)

  case POST -> Path("customers") =>
    val customer = Request.current.bodyJson[Customer]
    ctx.run {
      sql"""
      INSERT INTO customers(name)
      VALUES (${customer.name})
      """.insert()
    }
    Response.withBody(customer)

  case GET -> _ =>
    Response.withBody("Try http://localhost:8181/customers with GET or POST")

PotatehSharafServer(routes, host = "localhost", port = 8181).start()

println(s"Server started at http://localhost:8181")
```
<!-- example:end -->

Run the server:

```sh
scala sql_db.sc
```

In another terminal, add a customer:

```sh
curl -X POST http://localhost:8181/customers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Bob"}'
```

Then read the customer names:

```sh
curl http://localhost:8181/customers
```

The response contains `Bob`. Stop the server with Ctrl-C. The database lives in memory and is recreated when the server starts.
