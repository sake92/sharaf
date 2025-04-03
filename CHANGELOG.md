
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
