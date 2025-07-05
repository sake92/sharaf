---
title: Routes Matching
description: Sharaf Routes Matching
---

# {{ page.title }}


## Routes matching design

    Web frameworks do their routes matching with various mechanisms:
    - annotation + method param: [Spring](https://spring.io/guides/tutorials/rest/) and most other popular Java frameworks, [Cask](https://com-lihaoyi.github.io/cask/) etc
    - special route file DSL: [PlayFramework](https://www.playframework.com/documentation/2.9.x/ScalaRouting#The-routes-file-syntax), Ruby on Rails
    - in-language DSL: zio-http, akka-http
    - pattern matching: Sharaf, http4s


## Why not annotations?

Let's see an example:
```scala
@GetMapping(value = "/student/{studentId}")
public Student studentData1(@PathVariable Integer studentId) {}

@GetMapping(value = "/student/{studentId}")
public Student studentData2(@PathVariable Integer studentId) {}

@GetMapping(value = "/student/umm")
public Student studentData3(@PathVariable Integer studentId) {}
```
Issues:
- the `studentId` appears in 2 places, you can make a typo and nothing will work.
- the `"/student/{studentId}"` route is duplicated, there is no compiler support and it will fail only in runtime..
- you have to [wonder](https://stackoverflow.com/questions/2326912/ordered-requestmapping-in-spring-mvc) if `studentData1` will be picked up before `studentData3`..!?


## Why not special route file?
Well, you need a special compiler for this, essentially a new language.  
People have to learn how it works, there's probably no syntax highlighting, no autocomplete etc.


## Why not in-language DSL?
Similar to special route file approach, people need to learn it.  
And again, you don't leverage compiler's support like exhaustive pattern matching and extractors.
 

## Sharaf's approach

Sharaf does its route matching in plain scala code.  
Scala's pattern matching warns you when you have *duplicate routes*, or *impossible* routes.  
For example, if you write this:
```scala
case GET -> Path("cars", brand) => ???
case GET -> Path("cars", model) => ??? // Unreachable case

case GET -> Path("files", segments*) => ???
case GET -> Path("files", "abc.txt") => ??? // Unreachable case
```
you will get nice warnings, thanks compiler!

---
You can extract path variables with pattern matching:
```scala
case GET -> Path("cars", param[Int](carId)) => ???
```
Here, the `carId` is parsed as `Int` and it *mentioned only once*, unlike with the annotation approach.

