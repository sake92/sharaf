//> using scala 3.7.0
//> using dep com.h2database:h2:2.4.240
//> using dep ba.sake::sharaf-undertow:0.18.0
//> using dep ba.sake::squery:0.8.2

import ba.sake.tupson.JsonRW
import ba.sake.squery.{*, given}
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

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

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")
