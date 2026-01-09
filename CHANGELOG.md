
## 0.16.0
- add static files handler
- add classpath resources handler

## 0.15.0
- support sequences of case classes in formson
- add sun net httpserver impl

## 0.14.0
- SSE support
- support for named tuples in query params
- support for named tuples in form params
- support for union types in query params
- support for union types in form params
- add http4s module

## 0.12.1
- add SharafHandler to support middleware logic
- add twirl support with html and xml interpolators

## 0.11.1
- add snunit

## 0.10.0
- add sharaf-helidon
- replace requests with sttp4

## 0.9.3
- support stream resources via geny library
- add cookie utils
- add basic session utils

## 0.9.2
- add support for geny.Writable for streaming responses
- add `bimap` on FormDataRW and QueryStringRW

## 0.9.0
- add HttpMethod enum (breaking change)
  - You can use this regex to find: `case (GET|POST|PUT|DELETE|OPTIONS|PATCH)\(\)` and replace: `case $1`
- add support for response body InputStream
- add support for scalatags doctype

## 0.8.2
- support WebJars

## 0.7.4
- support polymorphic forms

## 0.5.0
- add http header utils

## 0.1.0
- added HTMX utils
- added CORS handler
- improved docs


## 0.0.1
First release.
