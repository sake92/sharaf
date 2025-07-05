---
title: Query Params Handling
description: Sharaf Query Params Handling
---

# {{ page.title }}


## Query params handling design

Web frameworks do handle query params with various mechanisms:
- annotation + method param: [Spring](https://spring.io/guides/tutorials/rest/) and most other popular Java frameworks, [Cask](https://com-lihaoyi.github.io/cask/) etc
- special route file DSL: [PlayFramework](https://www.playframework.com/documentation/2.9.x/ScalaRouting#The-routes-file-syntax), Ruby on Rails
- in-language DSL: zio-http
- pattern matching: http4s
- parsing from request: Sharaf


## Why not annotations?

This approach is mostly fine, as long as you know from where a parameter comes.

In Spring you use the `@RequestParam` annotation when you have simple parameters.  
But when you want to group them in a class [you don't use it](https://stackoverflow.com/questions/16942193/spring-mvc-complex-object-as-get-requestparam).. #wtf  
Also, that same class can be bound from the form body too... convenient? eh.

In [Cask](https://com-lihaoyi.github.io/cask/#variable-routes) there is no annotation, so it is ambiguous in my opinion.


## Why not special route file?
You need a special compiler for this, essentially a new language.  
People have to learn how it works, there's probably no syntax highlighting, no autocomplete etc.


## Why not in-language DSL?

Similar to special route file approach, people need to learn it.  
Not a huge deal I guess.


## Why not pattern matching?
If you look at [http4s' approach](https://http4s.org/v0.23/docs/dsl.html#handling-query-parameters),
you can see that if the query param is not found, it falls through.  
It is customizable, but more work for you. eh.  
Essentially you'll get a 404.. which is not a good choice IMO.

Rarely any framework does this, and you rarely want to handle *the same path* in 2 places.


## Sharaf's approach
    
Sharaf parses query params from the `Request`.  
Admittedly, you do have to make a new class if you want to parse them in a typesafe way.  
But you usually do grouping of these parameters when passing them further, so why not do it immediatelly.  

[Composition](/howtos/query-params.html#how-to-bind-composite-query-parameter) adds even more benefits, which I rarely saw implemented in any framework.
