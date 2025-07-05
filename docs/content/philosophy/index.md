---
title: Philosophy
description: Sharaf Philosophy
pagination:
  sort_by: title
---

# {{ page.title }}

## Why Sharaf?

Simplicity and ease of use is the main focus of Sharaf.  

Sharaf is built on top of [Undertow](https://undertow.io/).  
This means you can use awesome libraries built for Undertow, like [pac4j](https://github.com/pac4j/undertow-pac4j) for security and others.  
You can leverage Undertow's lower level API, e.g. for WebSockets.

Sharaf bundles a set of standalone libraries:
- [querson]({{site.data.project.gh.sourcesUrl}}/querson) for query parameters
- [tupson](https://github.com/sake92/tupson) for JSON
- [formson]({{site.data.project.gh.sourcesUrl}}/formson) for forms
- [validson]({{site.data.project.gh.sourcesUrl}}/validson) for validation
- [scalatags](https://github.com/com-lihaoyi/scalatags) for HTML
- [sttp](https://sttp.softwaremill.com/en/latest/) for firing HTTP requests
- [typesafe-config](https://github.com/lightbend/config) for configuration

You can use any of above separately in your projects.


## Why name "Sharaf"

Šaraf means "a screw" in Bosnian, which reminds me of scala spiral logo.  
It's a germanism I think.
