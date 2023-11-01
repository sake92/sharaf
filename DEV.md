


```sh

./mill clean

./mill __.reformat

./mill __.test

./mill examples.runMain bla

# for local dev/test
./mill __.publishLocal

git diff
git commit -am "msg"

$VERSION="0.0.11"
git tag -a $VERSION -m "Release $VERSION"
git push  --atomic origin main $VERSION
```

# TODOs

- rethrow WRAPPED parsing exceptions from Request
- add validson utils like min, max etc

- config read with JsonRW example
- add Docker / Watchtower example
- full-stack backend example with squery , flyway, Docker / Watchtower
- spring pet clinic implementation

- cookies ?


---
---

# Why nots

## Async frameworks like Play Framework, Akka HTTP etc
Synchronous programming is much, much easier to understand, debug, profile etc..  
Benefits (performance/throughput) of async handling are mostly void in Java 21, with introduction of Virtual threads. Yay! 

Only bummer for now is that Undertow doesn't still support them.. :/  
But undertow is performant in the current shape too, so for most use cases it will be enough.

## Pure FP libs like http4s, zio-http etc

Too much focus on purely functional programming and (mostly unnecessarry) math concepts.  
Easy to get lost in that and overcomplicate your code.

## Enterprisey Java frameworks like Spring Framework, Quarkus etc
Too much annotations, autoconfigurations, dependency injection and complexity.

## Standalone JEE servers like Tomcat, Jetty etc
I was looking into these, but then sharaf would have to depend on Servlets API,  
use `@Inject` and gazzilion of god-knows-what-they-do annotations just to configure OAuth2 for example...
