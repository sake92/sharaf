package files.tutorials

import utils.*
import Bundle.*

object SqlDb extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("SQL DB")

  override def blogSettings =
    super.blogSettings.withSections(dbSetup, squerySetup, routesSetup, runSection)

  val dbSetup = Section(
    "DB setup",
    s"""
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
    """.md
  )

  val squerySetup = Section(
    "Squery setup",
    s"""
    Sharaf recommends the [Squery](https://sake92.github.io/squery/) library for accessing databases with a JDBC driver.

    Create a file `sql_db.sc` and paste this code into it:
    ```scala
    //> using scala "3.3.1"
    //> using dep org.postgresql:postgresql:42.7.1
    //> using dep com.zaxxer:HikariCP:5.1.0
    //> using dep ba.sake::sharaf:${Consts.ArtifactVersion}
    //> using dep ba.sake::squery:0.0.16

    import io.undertow.Undertow
    import ba.sake.tupson.JsonRW
    import ba.sake.squery.*
    import ba.sake.sharaf.*, routing.*

    val ds = com.zaxxer.hikari.HikariDataSource()
    ds.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres")
    ds.setUsername("postgres")
    ds.setPassword("mysecretpassword")

    val ctx = new SqueryContext(ds)
    ```

    Here we set up the `SqueryContext` which we can use for accessing the database.
    """.md
  )

  val routesSetup = Section(
    "Querying",
    s"""
    Now we can do some querying on the db:
    ```scala
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
          sql${Consts.tq}
          INSERT INTO customers(name) 
          VALUES ($${customer.name})
          ${Consts.tq}.insert()
        }
        Response.withBody(customer)
    ```
    """.md
  )

  val runSection = Section(
    "Running the server",
    s"""
    Finally, we need to start up the server:
    ```scala
    Undertow
      .builder
      .addHttpListener(8181, "localhost")
      .setHandler(
        SharafHandler(routes).withErrorMapper(ErrorMapper.json)
      )
      .build
      .start()

    println(s"Server started at http://localhost:8181")
    ```

    and run it like this:
    ```sh
    scala-cli sql_db.sc 
    ```

    Then you can try the following requests:
    ```sh
    # get all customers
    curl http://localhost:8181/customers

    # add a customer
    curl --request POST \\
      --url http://localhost:8181/customers \\
      --data '{
        "name": "Bob"
      }'

    ```
    """.md
  )
}
