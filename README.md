
# Sharaf

Simple, intuitive, batteries-included HTTP server library.

## Usage
Mill:
```scala
def ivyDeps = Agg(
  ivy"ba.sake::sharaf:0.0.5"
)
def scalacOptions = Seq(
  "-Yretain-trees"
)
```

## Examples
- handling [json](examples/json)
- handling [form data](examples/form)
- rendering [html](examples/html) and serving static files
- implementation of [todobackend.com](examples/todo) featuring CORS handling
- [OAuth2 login](examples/oauth2) with [Pac4J library](https://www.pac4j.org/)


## Why sharaf?

Simplicity and ease of use is the main focus of sharaf.  

It is built on top of [undertow](https://undertow.io/).  
This means you can use awesome libraries built for undertow, like [pac4j](https://github.com/pac4j/undertow-pac4j) for security and similar.  
Also, you can leverage undertow's lower level API, e.g. for WebSockets.

Sharaf bundles a set of libraries:
- [querson](./querson) for query parameters
- [tupson](https://github.com/sake92/tupson) for JSON
- [formson](./formson) for forms
- [validson](./formson) for validation
- [hepek-components](https://github.com/sake92/hepek) for HTML (with [scalatags](https://github.com/com-lihaoyi/scalatags))
- [requests](https://github.com/com-lihaoyi/requests-scala) for firing HTTP requests


## Misc

Why name "sharaf"?  

Šaraf means a "screw" in Bosnian, which reminds me of scala spiral logo.

