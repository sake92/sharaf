---
title: JSON
description: Sharaf Tutorial JSON
---

# {{ page.title }}


## Model definition

Let's make a simple JSON API in scala-cli.  
Create a file `json_api.sc` and paste this code into it:
```scala
//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.12.1

import ba.sake.tupson.JsonRW
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

case class Car(brand: String, model: String, quantity: Int) derives JsonRW

object CarsDb {
  var db: Seq[Car] = Seq()
  def findAll(): Seq[Car] = db
  def findByBrand(brand: String): Seq[Car] = db.filter(_.brand == brand)
  def add(car: Car): Unit = db = db.appended(car)
}
```

Here we defined a `Car` model, which `derives JsonRW`, so we can use the JSON support from Sharaf.

We also use a `var db: Seq[Car]` to store our data.  
(don't do this for real projects)


## Routes definition
Next step is to define a few routes for getting and adding cars:
```scala
val routes = Routes:  
  case GET -> Path("cars") =>
    Response.withBody(CarsDb.findAll())

  case GET -> Path("cars", brand) =>
    val res = CarsDb.findByBrand(brand)
    Response.withBody(res)

  case POST -> Path("cars") =>
    val reqBody = Request.current.bodyJson[Car]
    CarsDb.add(reqBody)
    Response.withBody(reqBody)
```

The first route returns all data in the database.  

The second route does some filtering on the database.  

The third route binds the JSON body from the HTTP request.  
Then we add it to the database.


## Running the server

Finally, start up the server:
```scala
UndertowSharafServer("localhost", 8181, routes, exceptionMapper = ExceptionMapper.json).start()

println("Server started at http://localhost:8181")
```

and run it like this:
```sh
scala json_api.sc 
```

Then try the following requests:
```sh
# get all cars
curl http://localhost:8181/cars

# add a car
curl --request POST \
    --url http://localhost:8181/cars \
    --data '{
    "brand": "Mercedes",
    "model": "ML350",
    "quantity": 1
    }'

# get cars by brand
curl http://localhost:8181/cars/Mercedes
```

