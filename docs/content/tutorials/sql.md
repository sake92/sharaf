---
title: SQL
description: Sharaf Tutorial SQL
---

# {{ page.title }}


## DB setup

Create a new Postgres database with Docker:
```sh
docker run --name sharaf-postgres -e POSTGRES_PASSWORD=mysecretpassword -p 5432:5432 -d postgres
```

Then connect to it via `psql` (or your favorite SQL tool):
```sh
docker exec -it sharaf-postgres psql -U postgres postgres
```
and create a table:
```sql
CREATE TABLE customers(
    id SERIAL PRIMARY KEY,
    name VARCHAR
);
```

## Squery setup
Sharaf recommends the [Squery](https://sake92.github.io/squery/) library for accessing databases with a JDBC driver.

Create a file `sql_db.sc` and paste this code into it:
```scala
//> using scala "3.7.0"
//> using dep org.postgresql:postgresql:42.7.5
//> using dep com.zaxxer:HikariCP:6.3.0
//> using dep {{site.data.project.artifact.org}}::{{site.data.project.artifact.name}}:{{site.data.project.artifact.version}}
//> using dep ba.sake::squery:0.7.0

import ba.sake.tupson.JsonRW
import ba.sake.squery.{*, given}
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val ds = com.zaxxer.hikari.HikariDataSource()
ds.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres")
ds.setUsername("postgres")
ds.setPassword("mysecretpassword")

val ctx = new SqueryContext(ds)
```

Here we set up the `SqueryContext` which we can use for accessing the database.


## Querying
Now we can do some querying on the db:
```scala
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

```


## Running the server

Finally, we need to start up the server:
```scala
UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")
```

and run it like this:
```sh
scala sql_db.sc 
```

Then you can try the following requests:
```sh
# get all customers
curl http://localhost:8181/customers

# add a customer
curl --request POST \
    --url http://localhost:8181/customers \
    --data '{
    "name": "Bob"
    }'
```
