---
layout: tutorial.html
title: SQL
description: Store and read data with Sharaf and Squery
---

# {{ page.title }}

In this lesson, you will add a customer and read it back. The example uses an in-memory H2 database, so no database setup is needed.

Create a file named `sql_db.sc`:

```scala
{% include "sql_db.sc" %}
```

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
