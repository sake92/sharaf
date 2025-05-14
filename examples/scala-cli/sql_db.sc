//> using scala "3.7.0"
//> using dep org.postgresql:postgresql:42.7.5
//> using dep com.zaxxer:HikariCP:6.3.0
//> using dep ba.sake::sharaf-undertow:0.10.0
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

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")
