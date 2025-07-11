---
title: Quickstart
description: Sharaf Tutorial Quickstart
---

# {{ page.title }}

Get started quickly with Sharaf framework.

## Mill

```scala
def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"{{site.data.project.artifact.org}}::{{site.data.project.artifact.name}}:{{site.data.project.artifact.version}}"
)
def scalacOptions = super.scalacOptions() ++ Seq("-Yretain-trees")
```

## Sbt

```scala
libraryDependencies ++= Seq(
    "{{site.data.project.artifact.org}}" %% "{{site.data.project.artifact.name}}" % "{{site.data.project.artifact.version}}"
),
scalacOptions ++= Seq("-Yretain-trees")
```


## Scala CLI

Create a file `my_script.sc` with the following content:
```scala
//> using dep {{site.data.project.artifact.org}}::{{site.data.project.artifact.name}}:{{site.data.project.artifact.version}}
```
and then run it with:
```bash
scala my_script.sc --scala-option -Yretain-trees
```


## Examples

- [Scala CLI examples]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli), standalone examples using Scala CLI
- [Scala CLI HTMX examples]({{site.data.project.gh.sourcesUrl}}/examples/htmx), standalone examples featuring HTMX
- [API example]({{site.data.project.gh.sourcesUrl}}/examples/api) featuring JSON and validation
- [full-stack example]({{site.data.project.gh.sourcesUrl}}/examples/fullstack) featuring HTML, static files and forms
- [sharaf-todo-backend](https://github.com/sake92/sharaf-todo-backend), implementation of the [todobackend.com](http://todobackend.com/) spec, featuring CORS handling
- [Username+Password form login]({{site.data.project.gh.sourcesUrl}}/examples/user-pass-form) with [Pac4J](https://www.pac4j.org/)
- [JWT auth]({{site.data.project.gh.sourcesUrl}}/examples/jwt) with [Pac4J](https://www.pac4j.org/)
- [OAuth2 login]({{site.data.project.gh.sourcesUrl}}/examples/oauth2) with [Pac4J](https://www.pac4j.org/)
- [Snunit]({{site.data.project.gh.sourcesUrl}}/examples/snunit) demo app
- [PetClinic](https://github.com/sake92/sharaf-petclinic) implementation, featuring full-stack app with Postgres db, config, integration tests etc.
- [Giter8 template for fullstack app](https://github.com/sake92/sharaf-fullstack.g8)
