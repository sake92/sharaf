//> using scala "3.4.2"
//> using dep org.postgresql:postgresql:42.7.1
//> using dep com.zaxxer:HikariCP:5.1.0
//> using dep ba.sake::sharaf:0.8.0
//> using dep ba.sake::squery:0.3.0

import io.undertow.Undertow
import ba.sake.tupson.JsonRW
import ba.sake.squery.{*, given}
import ba.sake.sharaf.*, routing.*

val ds = com.zaxxer.hikari.HikariDataSource()
ds.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres")
ds.setUsername("postgres")
ds.setPassword("mysecretpassword")

val ctx = new SqueryContext(ds)

case class Customer(name: String) derives JsonRW

val routes = Routes:
  case GET() -> Path("customers") =>
    val customerNames = ctx.run {
      sql"SELECT name FROM customers".readValues[String]()
    }
    Response.withBody(customerNames)

  case POST() -> Path("customers") =>
    val customer = Request.current.bodyJson[Customer]
    ctx.run {
      sql"""
      INSERT INTO customers(name) 
      VALUES (${customer.name})
      """.insert()
    }
    Response.withBody(customer)

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
