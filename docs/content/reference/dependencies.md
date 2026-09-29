---
title: Dependencies
description: Sharaf artifact coordinates and build settings
---

# {{ page.title }}

The examples use Scala 3.9.0 and Sharaf {{site.data.project.artifact.version}}. Artifact group: `{{site.data.project.artifact.org}}`.

| Build tool | Undertow dependency |
| --- | --- |
| Scala CLI | `//> using dep {{site.data.project.artifact.org}}::{{site.data.project.artifact.name}}:{{site.data.project.artifact.version}}` |
| sbt | `"{{site.data.project.artifact.org}}" %% "{{site.data.project.artifact.name}}" % "{{site.data.project.artifact.version}}"` |
| Mill | `ivy"{{site.data.project.artifact.org}}::{{site.data.project.artifact.name}}:{{site.data.project.artifact.version}}"` |

For sbt, add `scalacOptions += "-Yretain-trees"` where derivation uses default values. For Mill, add `def scalacOptions = super.scalacOptions() ++ Seq("-Yretain-trees")` to the module. Scala CLI accepts `//> using options -Yretain-trees` in a source file.

The [servers and platforms reference](/reference/servers.html) lists other Sharaf artifacts.
