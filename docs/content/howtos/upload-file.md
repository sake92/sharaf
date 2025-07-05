---
title: Upload File
description: Sharaf How To Upload File
---

# {{ page.title }}

Uploading a file is usually done via `multipart/form-data` form submission.  


```scala
// 1. somewhere in a view, use enctype="multipart/form-data"
form(action := "/form-submit", method := "POST", enctype := "multipart/form-data")(
    ...
)

// 2. define form data class with a NIO Path file
import java.nio.file.Path
import ba.sake.formson.*

case class MyData(file: Path) derives FormDataRW

// 3. handle the file however you want
case POST -> Path("form-submit") =>
  val formData = Request.current.bodyForm[MyData]
  val fileAsString = Files.readString(formData.file)
```

You can find a working example in the [repo]({{site.data.project.gh.sourcesUrl}}/examples/fullstack).
