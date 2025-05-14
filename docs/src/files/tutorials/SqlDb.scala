package files.tutorials

import utils.*

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

  private val snip1 = ScalaCliFiles.sql_db.snippet(until = "case class Customer").indent(4)
  private val snip2 = ScalaCliFiles.sql_db
    .snippet(from = "case class Customer", until = "UndertowSharafServer")
    .indent(4)
    .trim
  private val snip3 = ScalaCliFiles.sql_db.snippet(from = "UndertowSharafServer").indent(4)

  val squerySetup = Section(
    "Squery setup",
    s"""
    Sharaf recommends the [Squery](https://sake92.github.io/squery/) library for accessing databases with a JDBC driver.

    Create a file `sql_db.sc` and paste this code into it:
    ```scala
    ${snip1}
    ```

    Here we set up the `SqueryContext` which we can use for accessing the database.
    """.md
  )

  val routesSetup = Section(
    "Querying",
    s"""
    Now we can do some querying on the db:
    ```scala
    ${snip2}
    ```
    """.md
  )

  val runSection = Section(
    "Running the server",
    s"""
    Finally, we need to start up the server:
    ```scala
    ${snip3}
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
